import numpy as np
import pandas as pd

from matplotlib import pyplot as plt

import seaborn as sns

sns.set_style('whitegrid')

import os
import struct

from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import OneHotEncoder
from sklearn.metrics import precision_recall_curve
from sklearn.metrics import confusion_matrix


def one_hot_encoding(X, df, name):
    unique_attr = set(df[name])

    for att in unique_attr:
        e = df[name] == att
        e = np.array(e).astype('int')
        e = np.expand_dims(e, axis=1)
        X = np.hstack((X, e))

    return X


def plot_confusion_matrix(data, title='Confusion matrix', cmap=plt.cm.cool):
    plt.clf()
    data = data / np.linalg.norm(data)
    plt.matshow(data, cmap=cmap)
    plt.colorbar()


def read_data(training, output_column, columns, standardize, output_one_hot, input_one_hot=[]):
    if training:
        df = pd.read_csv('/data/house_data_train.csv')
    else:
        df = pd.read_csv('/data/house_data_test.csv')

    # create np array
    X = np.array(df[columns])

    # standardize input
    if standardize:
        X = (X - X.mean(axis=0)) / X.std(axis=0)

    # input to onehot
    for ioh in input_one_hot:
        X = one_hot_encoding(X, df, ioh)

    # encode output one-hot
    y = df[output_column]
    if output_one_hot:
        classes = list(set(df[output_column]))
        y = np.array([np.eye(len(classes))[classes.index(x)] for x in df[output_column]])

    return X, y
