import scipy
from scipy.io import wavfile
from scipy import signal
import numpy as np
import matplotlib.pyplot as plt

# read wav files
rateLL, f = wavfile.read('audio/iphone_recording/6_left/LL.wav', mmap=True)
rateLU, g = wavfile.read('audio/iphone_recording/6_left/LU.wav', mmap=True)

print('Rate LL: %s' % rateLL)
print('Rate LU: %s' % rateLU)

x = np.arange(0, f.shape[0])

cross = signal.correlate(f, g, mode='full')

pltCount = 3

plt.subplot(pltCount, 1, 1)
plt.title('Lower Left')
plt.plot(x, f)

plt.subplot(pltCount, 1, 2)
plt.title('Upper Left')
plt.plot(x, g)

plt.subplot(pltCount, 1, 3)
plt.title('Cross Correlation')
plt.plot(cross)

plt.show()
