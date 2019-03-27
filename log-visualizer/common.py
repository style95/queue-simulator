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
    activations = pd.read_csv(folder + '/activations.csv')
    return format_data(snapshots, activations)


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


def plot_histogram(df, figsize=(10, 4), percentile=95):
    fig, axes = plt.subplots(1, 2, figsize=figsize)
    fig.subplots_adjust(wspace=0.4)

    for metric, ax in zip(histogram_metrics, axes.flatten()):
        y = df[metric] / 1e6  # convert to ms
        _, bins, _ = ax.hist(
            y, bins=80, range=(y.min(), np.percentile(y, percentile)),
            color='orange'
        )
        # add_mean_bar(y, ax)
        ax.grid(axis='x')
        ax.set_title(metric)
        ax.set_xlabel('ms')
        ax.set_ylabel('number')

        right = ax.twinx()
        right.hist(
            y, bins=bins, density=True, cumulative=True,
            histtype='step', linewidth=2
        )
        right.grid(axis='y')
        right.set_ylabel('likelihood')
    plt.show()
    return fig


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