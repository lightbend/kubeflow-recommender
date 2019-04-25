#!/usr/bin/env python
# coding: utf-8

# # This is implementation of the Recommender training
# 
# This implementation takes a list of users and their purchasing history to calculate prediction
# on the probability that they would by a certain product.
# The implementation is structured in 2 parts:
# 1. Build rating matrix based on the purchasing history. The implementation is based on this blog post
# https://medium.com/datadriveninvestor/how-to-build-a-recommendation-system-for-purchase-data-step-by-step-d6d7a78800b6
# 2. Build collabarative filtering model based on the rating matrix. The implementation is based on this project https://github.com/Piyushdharkar/Collaborative-Filtering-Using-Keras 
# 
# Implementation is leveraging Minio for storing both source data and result models
# 

# Install libraries

import pandas as pd
import numpy as np
from minio import Minio
from keras.models import Model
from keras.layers import *
from keras.losses import *
import tensorflow as tf
from keras import backend as K
import os

# Read data

minio_endpoint = os.environ.get('MINIO_URL', 'minio-service-kubeflow.lightshift.lightbend.com')
minio_key = os.environ.get('MINIO_KEY', 'minio')
minio_secret = os.environ.get('MINIO_SECRET', 'minio123')

print('Minio parameters : URL ', minio_endpoint, ' key ', minio_key, ' secret ', minio_secret)

os.environ['AWS_ACCESS_KEY_ID'] = minio_key
os.environ['AWS_SECRET_ACCESS_KEY'] = minio_secret
os.environ['AWS_REGION'] = 'us-west-1'
os.environ['S3_REGION'] = 'us-west-1'
os.environ['S3_ENDPOINT'] = minio_endpoint
os.environ['S3_USE_HTTPS'] = '0'
os.environ['S3_VERIFY_SSL'] = '0'

minioClient = Minio(minio_endpoint,
                    access_key=minio_key,
                    secret_key=minio_secret,
                    secure=False)

minioClient.fget_object('data', 'recommender/users.csv', '/tmp/users.csv')
customers = pd.read_csv('/tmp/users.csv')
minioClient.fget_object('data', 'recommender/transactions.csv', '/tmp/transactions.csv')
transactions = pd.read_csv('/tmp/transactions.csv')

print(customers.shape)
customers.head()


print(transactions.shape)
transactions.head()


# Data preparation

transactions['products'] = transactions['products'].apply(lambda x: [int(i) for i in x.split('|')])
transactions.head(2).set_index('customerId')['products'].apply(pd.Series).reset_index()

pd.melt(transactions.head(2).set_index('customerId')['products'].apply(pd.Series).reset_index(),
             id_vars=['customerId'],
             value_name='products') \
    .dropna().drop(['variable'], axis=1) \
    .groupby(['customerId', 'products']) \
    .agg({'products': 'count'}) \
    .rename(columns={'products': 'purchase_count'}) \
    .reset_index() \
    .rename(columns={'products': 'productId'})


# Create data with user, item, and target field

data = pd.melt(transactions.set_index('customerId')['products'].apply(pd.Series).reset_index(),
             id_vars=['customerId'],
             value_name='products') \
    .dropna().drop(['variable'], axis=1) \
    .groupby(['customerId', 'products']) \
    .agg({'products': 'count'}) \
    .rename(columns={'products': 'purchase_count'}) \
    .reset_index() \
    .rename(columns={'products': 'productId'})
data['productId'] = data['productId'].astype(np.int64)

print(data.shape)
data.head()

def create_data_dummy(data):
    data_dummy = data.copy()
    data_dummy['purchase_dummy'] = 1
    return data_dummy

data_dummy = create_data_dummy(data)


# Normalize item values across users

df_matrix = pd.pivot_table(data, values='purchase_count', index='customerId', columns='productId')
df_matrix.head()

df_matrix_norm = (df_matrix-df_matrix.min())/(df_matrix.max()-df_matrix.min())
print(df_matrix_norm.shape)
df_matrix_norm.head()

d = df_matrix_norm.reset_index()
d.index.names = ['scaled_purchase_freq']
data_norm = pd.melt(d, id_vars=['customerId'], value_name='scaled_purchase_freq').dropna()
print(data_norm.shape)
data_norm.head()

# Preparing data for learning

customer_idxs = np.array(data_norm.customerId, dtype = np.int)
product_idxs = np.array(data_norm.productId, dtype = np.int)

ratings = np.array(data_norm.scaled_purchase_freq)

n_customers = int(data_norm['customerId'].drop_duplicates().max()) + 1
n_products = int(data_norm['productId'].drop_duplicates().max()) + 1
n_factors = 50

input_shape = (1,)

print(n_customers)
print(n_products)
print(customer_idxs)
print(product_idxs)
print(ratings)

# Tensorflow Session

sess = tf.Session()
K.set_session(sess)
K.set_learning_phase(1)

# Model Class

class DeepCollaborativeFiltering(Model):
    def __init__(self, n_customers, n_products, n_factors, p_dropout = 0.2):
        x1 = Input(shape = (1,), name="user")

        P = Embedding(n_customers, n_factors, input_length = 1)(x1)
        P = Reshape((n_factors,))(P)

        x2 = Input(shape = (1,), name="product")

        Q = Embedding(n_products, n_factors, input_length = 1)(x2)
        Q = Reshape((n_factors,))(Q)

        x = concatenate([P, Q], axis=1)
        x = Dropout(p_dropout)(x)

        x = Dense(n_factors)(x)
        x = Activation('relu')(x)
        x = Dropout(p_dropout)(x)

        output = Dense(1)(x)       
        
        super(DeepCollaborativeFiltering, self).__init__([x1, x2], output)
    
    def rate(self, customer_idxs, product_idxs):
        if (type(customer_idxs) == int and type(product_idxs) == int):
            return self.predict([np.array(customer_idxs).reshape((1,)), np.array(product_idxs).reshape((1,))])
        
        if (type(customer_idxs) == str and type(product_idxs) == str):
            return self.predict([np.array(customerMapping[customer_idxs]).reshape((1,)), np.array(productMapping[product_idxs]).reshape((1,))])
        
        return self.predict([
            np.array([customerMapping[customer_idx] for customer_idx in customer_idxs]), 
            np.array([productMapping[product_idx] for product_idx in product_idxs])
        ])

# Hyperparameters

bs = 64
val_per = 0.25
epochs = 3

# Definition

model = DeepCollaborativeFiltering(n_customers, n_products, n_factors)
model.summary()

# Training

model.compile(optimizer = 'adam', loss = mean_squared_logarithmic_error)
model.fit(x = [customer_idxs, product_idxs], y = ratings, batch_size = bs, epochs = epochs, validation_split = val_per)
print('Done training!')

print ("input 0", model.input[0].name)
print ("input 1", model.input[1].name)
print ("input ", model.input)

print ("output 0", model.output[0].name)
print ("output 1", model.output[1].name)
print ("output", model.output)


# Get current output directory for model

directorystream = minioClient.get_object('data', 'recommender/directory.txt')
directory = ""
for d in directorystream.stream(32*1024):
    directory += d.decode('utf-8')
arg_version = "1"    
export_path = 's3://models/' + directory + '/' + arg_version + '/'
print ('Exporting trained model to', export_path)

# Export models

tensor_info_ver = tf.saved_model.utils.build_tensor_info(tf.constant([arg_version]))
# inputs/outputs
tensor_info_users = tf.saved_model.utils.build_tensor_info(model.input[0])
tensor_info_products = tf.saved_model.utils.build_tensor_info(model.input[1])
tensor_info_pred = tf.saved_model.utils.build_tensor_info(model.output)
# signature
prediction_signature = (tf.saved_model.signature_def_utils.build_signature_def(
        inputs={'users': tensor_info_users, 'products': tensor_info_products},
        outputs={'recommendations': tensor_info_pred,
                 'model-version': tensor_info_ver},
        method_name=tf.saved_model.signature_constants.PREDICT_METHOD_NAME))
# export
legacy_init_op = tf.group(tf.tables_initializer(), name='legacy_init_op')
builder = tf.saved_model.builder.SavedModelBuilder(export_path)
builder.add_meta_graph_and_variables(
      sess, [tf.saved_model.tag_constants.SERVING],
      signature_def_map={
           tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY: prediction_signature,
      },
      legacy_init_op=legacy_init_op)
builder.save()



