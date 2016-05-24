from flask import Flask

app = Flask(__name__)


@app.route('/')
def index():
    return 'Welcome to my Neuronal Network!'


@app.route('/add')
def index():
    return 'add route'


@app.route('/train')
def index():
    return 'training route'


@app.route('/predict')
def index():
    return 'predict route'


if __name__ == '__main__':
    app.run()
