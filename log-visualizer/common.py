import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

series_metrics = ['in', 'out', 'current', 'existing', 'creating', 'averageLatency']
histogram_metrics = ['timeToServe', 'timeOfInvoke']
should_smooth = ['in', 'out']


def read_tables(folder):
    if folder.endswith('/'):
        folder = folder[:-1]
    snapshots = pd.read_csv(folder + '/snapshots.csv')
    predictions = pd.read_csv(folder + '/predictions.csv')
    activations = pd.read_csv(folder + '/activations.csv')
    return format_data(snapshots, predictions, activations)


def format_data(*dfs):
    def convert(df):
        df['elapse'] = pd.to_timedelta(df['elapse'], unit='ns')
        # rolling window smooth requires a index of time deltas
        return df.set_index('elapse')

    return map(convert, dfs)


def plot_time_series(df, figsize=(12, 7)):
    fig, axes = plt.subplots(2, 3, figsize=figsize)
    fig.subplots_adjust(hspace=0.4)

    for metric, ax in zip(series_metrics, axes.flatten()):
        x = df.index.values / 1e6  # convert to ms
        y = df[metric]
        if metric in should_smooth:
            y = y.rolling('1s').sum()
        ax.plot(x, y)

        ax.grid(axis='both')
        ax.set_title(metric)
        ax.set_xlabel('ms')
    plt.show()
    return fig


def plot_predictions(observation, prediction, figsize=(12, 4)):
    fig, ax = plt.subplots(1, 1, figsize=figsize)
    
    ox = observation.index.values / 1e6 # convert to ms
    oy = observation['in'].rolling('1s').sum()
    oh, = ax.plot(ox, oy)
    
    px = prediction.index.values / 1e6
    py = prediction['predictedTps']
    ph, = ax.plot(px, py)
    
    ax.grid(axis='both')
    ax.set_title('Observed TPS and predicted TPS')
    ax.set_xlabel('ms')
    ax.legend((oh, ph), ('observed', 'predicted'))
    
    plt.show()
    return fig


def plot_histogram(df, figsize=(10, 4), percentile=95):
    fig, axes = plt.subplots(1, 2, figsize=figsize)
    fig.subplots_adjust(wspace=0.4)

    for metric, ax in zip(histogram_metrics, axes.flatten()):
        y = df[metric] / 1e6  # convert to ms
        range = (y.min(), np.percentile(y, percentile))
        _, bins, _ = ax.hist(
            y, bins=100, range=range, color='orange'
        )
        # add_mean_bar(y, ax)
        ax.grid(axis='x')
        ax.set_title(metric)
        ax.set_xlabel('ms')
        ax.set_ylabel('number')

        x, y = zoomed_cdf(y, percentile)
        
        right = ax.twinx()
        right.plot(x, y)
        right.grid(axis='y')
        right.set_ylabel('likelihood')
        right.set_ylim((0, 1))
    plt.show()
    return fig


def zoomed_cdf(data, percentile):
    nbins = 100
    left, right = data.min(), np.percentile(data, percentile)
    zoom_len = right - left
    yd, x = np.histogram(
        data, bins=nbins, range=(left, right), density=True
    )
    # normalize the density function, so that its cumulative sum is 1
    yd = yd / (nbins / zoom_len)
    yc = np.cumsum(yd) * (percentile / 100)
    
    return x[:-1], yc


def distribution_summary(df):
    def row(metric):
        x = df[metric] / 1e6 # convert to ms
        return {
            'metric': metric, 'mean': x.mean(), 
            'p90': np.percentile(x, 90), 'p95': np.percentile(x, 95),
            'p99': np.percentile(x, 99), 'p99.9': np.percentile(x, 99.9)
        }
    data = [row(m) for m in histogram_metrics]
    # make sure that the order of the columns is deterministic
    return pd.DataFrame(data, columns=[
        'metric', 'mean', 'p90', 'p95', 'p99', 'p99.9'
    ])


def add_mean_bar(x, ax):
    mean = x.mean()
    ax.axvline(mean, color='k', linestyle='dashed', linewidth=2)
    xmin, xmax = ax.get_xlim()
    _, ymax = ax.get_ylim()
    ax.text(mean + (xmax - xmin) * 0.05, ymax * 0.8, f'Mean: {mean:.2f}')