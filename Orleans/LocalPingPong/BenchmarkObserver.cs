using Orleans;
using Orleans.Concurrency;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace LocalPingPong
{
    public interface IBenchmarkObserver : IGrainObserver
    {
        void BenchmarkFinished();
    }

    public class BenchmarkObserver : IBenchmarkObserver
    {
        readonly TaskCompletionSource<bool> tcs = new TaskCompletionSource<bool>();

        public void BenchmarkFinished()
        {
            tcs.SetResult(true);
        }

        public Task<bool> AsTask()
        {
            return tcs.Task;
        }
    }
}
