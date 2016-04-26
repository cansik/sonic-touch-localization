import os
import matplotlib.pyplot as plt
from matplotlib import colors
import numpy as np


def readData(filename):
    with open(filename) as f:
        lines = f.readlines()
        values = map(lambda l: l.strip().split(','), lines)

        values = filter(lambda v: len(v) > 1, values)

        xv = map(lambda v: v[0], values)
        yv = map(lambda v: v[1], values)

        # subsample
        # xv = np.array(xv)[::2].tolist()
        yv = np.array(yv)[::1].tolist()
        xv = range(len(yv))

        return xv, yv


graph_colors = ['b', 'r', 'g', 'k', 'm']

maxY = 0
i = 1
plt.figure(1)
while os.path.exists('plot%s.data' % i):
    xv, yv = readData('plot%s.data' % i)
    maxY = len(yv)
    plt.subplot(410 + i)
    plt.plot(xv, yv, 'b.-', label='Plot %s' % i, color=graph_colors[i - 1])

    plt.ylim([-1.2, 1.2])
    plt.xlim([0, maxY])
    plt.ylabel('amplitude')
    plt.xlabel('time')
    plt.legend(loc='best')

    plt.title('Channel %s' % i)

    plt.grid()

    i += 1


maxY = 0
i = 1
plt.subplot(413)
while os.path.exists('plot%s.data' % i):
    xv, yv = readData('plot%s.data' % i)
    maxY = len(yv)
    plt.plot(xv, yv, 'b.-', label='Plot %s' % i, color=graph_colors[i - 1], alpha=0.5)

    plt.grid()

    i += 1

plt.ylim([-1.2, 1.2])
plt.xlim([0, maxY])
plt.ylabel('amplitude')
plt.xlabel('time')
plt.legend(loc='best')

plt.title('Both Channels')

#cross corrletation
xv, yv = readData('cross.data')
maxY = len(yv)
plt.subplot(414)
plt.plot(xv, yv, 'g.-', label='Cross Correlation', color=graph_colors[i - 1])

plt.ylim([-1.2, 1.2])
plt.xlim([0, maxY])
plt.ylabel('amplitude')
plt.xlabel('time')
plt.legend(loc='best')

plt.title('Channel %s' % i)

plt.grid()

plt.show()
