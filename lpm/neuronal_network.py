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


# Implementation Neural Network / utilities

class NeuralNetwork:
    def __init__(self, layers, learning_rate=0.1, epochs=100, seed=1):
        self._layers = layers
        self._weights = []
        self._learning_rate = learning_rate
        self._epochs = epochs

        # set seed
        np.random.seed(seed)

        # init weights between hidden layers
        for i in range(1, len(layers) - 1):
            self._weights.append(self._rnd((layers[i - 1] + 1, layers[i] + 1)))

        # init weight to output layer
        self._weights.append(self._rnd((layers[i] + 1, layers[i + 1])))

        # print("Shapes:\n%s" % "\n".join(map(lambda x: str(x.shape), self._weights)))

    def fit(self, X, y):
        X = self._add_bias(X)

        for j in range(self._epochs):
            for i in range(X.shape[0]):
                self._back_propagation(X, y, i)

    def _back_propagation(self, X, y, i):
        # select datapoint
        a = [X[i]]

        # forward propagation
        for l in range(len(self._weights)):
            node = a[l].dot(self._weights[l])
            node = self._sigmoid(node)
            a.append(node)

        # error of the output
        error = y[i] - a[-1]  # eventuell umdrehen?
        deltas = [error * self._derivative(a[-1])]

        # calculate deltas for hidden layers
        for l in range(len(a) - 2, 0, -1):
            deltas.append(deltas[-1].dot(self._weights[l].T) * self._derivative(a[l]))

        # reverse deltas
        deltas.reverse()

        # backpropagation
        for i in range(len(self._weights)):
            # calculate gradient
            layer = np.atleast_2d(a[i])
            delta = np.atleast_2d(deltas[i])

            # update weights
            self._weights[i] += self._learning_rate * layer.T.dot(delta)

    def predict(self, x):
        a = np.hstack((1., x))

        # forward propagation through all layers
        for l in range(len(self._weights)):
            a = self._sigmoid(np.dot(a, self._weights[l]))
        return a

    def predict_all(self, X):
        predicted = []

        # calculate results
        for i in range(X.shape[0]):
            p = self.predict(X[i])
            predicted.append(p)

        return np.vstack(predicted)

    @staticmethod
    def binarize_predicted(predicted):
        for p in predicted:
            m = 0
            index = -1
            for i in range(len(p)):
                if p[i] > m:
                    index = i
                    m = p[i]
                p[i] = 0
            p[index] = 1
        return predicted

    @staticmethod
    def join_binarize(predicted):
        result = list()
        for p in predicted:
            for i in range(len(p)):
                if p[i] == 1:
                    result.append(i)
                    break
        return np.array(result)

    @staticmethod
    def score(y_test, predicted):
        # calculate score
        from sklearn.metrics import accuracy_score
        return accuracy_score(y_test, predicted)

    @staticmethod
    def _rnd(size):
        return np.random.uniform(low=-0.7, high=0.7, size=size)

    @staticmethod
    def _add_bias(layer):
        ones = np.atleast_2d(np.ones(layer.shape[0]))
        return np.concatenate((ones.T, X), axis=1)

    @staticmethod
    def _sigmoid(x):
        return 1. / (1 + np.exp(-x))

    @staticmethod
    def _derivative(x):
        return x * (1 - x)

    @staticmethod
    def rmse(targets, predictions):
        return np.sqrt(np.mean((predictions - targets) ** 2))
