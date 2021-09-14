package my.cute.channelpoints.misc;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * simple wrapper for a ScheduledExecutorService that extends it to allow for "conditional" 
 * scheduled repeating tasks; ie, tasks that cancel themselves when some condition is true.
 * the typical {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
 * allows for cancellation of the submitted task via the returned ScheduledFuture, but it's
 * inconvenient for tasks which should repeat until some condition is true, since this 
 * condition is probably best seen from within the task and it's difficult/not possible to
 * cancel the returned future from within the task itself
 * <p>
 * all methods except for {@link #scheduleWithFixedDelayAndCondition(Callable, long, long, TimeUnit)}
 * and {@link #scheduleWithFixedDelayAndConditionAsync(Callable, long, long, TimeUnit)} simply
 * delegate to the wrapped {@link ScheduledExecutorService}
 */
public class ConditionalExecutorService implements ScheduledExecutorService {

	private static final Logger logger = LoggerFactory.getLogger(ConditionalExecutorService.class);
	private final ScheduledExecutorService inner;
	
	public ConditionalExecutorService(ScheduledExecutorService executor) {
		this.inner = executor;
	}
	
	@Override
	public void shutdown() {
		this.inner.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return this.inner.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return this.inner.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.inner.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return this.inner.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.inner.submit(task);
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return this.inner.submit(task, result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.inner.submit(task);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return this.inner.invokeAll(tasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return this.inner.invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return this.inner.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return this.inner.invokeAny(tasks, timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		this.inner.execute(command);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return this.inner.schedule(command, delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return this.inner.schedule(callable, delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return this.inner.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return this.inner.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}
	
	/**
	 * submits a periodic action that first runs after the given initial delay, and then after the
	 * given delay has elapsed since the completion of the previous run
	 * <p>
	 * basically the same thing as {@link #scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}, except
	 * a Callable is passed instead of a Runnable; if the Callable returns true, the task will continue
	 * as normal (ie it will run again after the given delay has elapsed), and if the Callable returns 
	 * false, the task will be cancelled and will not execute again
	 * @param action the task to execute. it will continue repeating with the given delay as long as it
	 * continues to return true; if it returns false, the task will not execute again
	 * @param initialDelay the delay until the first execution of the given task
	 * @param delay the delay between when one iteration of the task finishes and the next iteration
	 * begins
	 * @param unit the unit of the given delay
	 */
	public void scheduleWithFixedDelayAndCondition(Callable<Boolean> action, long initialDelay, long delay, TimeUnit unit) {
		this.inner.schedule(this.scheduledConditionalWrapper(action, delay, unit), initialDelay, unit);
	}
	
	/*
	 * in order to repeat the given action, on every iteration we retrieve the result, and if it's 
	 * true we schedule another iteration. easy enough? handling the exception from Callable is
	 * kind of weird though
	 */
	private Runnable scheduledConditionalWrapper(Callable<Boolean> action, long delay, TimeUnit unit) {
		return (() -> {
			boolean result;
			try {
				result = action.call();
			} catch (Exception e) {
				//should this be done differently?
				logger.warn("exception thrown in scheduled repeating conditional", e);
				return;
			}
			if(result) {
				this.inner.schedule(this.scheduledConditionalWrapper(action, delay, unit), delay, unit);
			} 
		});
	}
	
	/**
	 * submits a periodic action that first runs after the given initial delay, and then after the
	 * given delay has elapsed since the completion of the previous run
	 * <p>
	 * basically the same as {@link #scheduleWithFixedDelayAndCondition(Callable, long, long, TimeUnit)},
	 * except allowing for the use of CompletableFutures inside the given Callable. this could be useful if
	 * a repeating task requires async logic in which the true/false result of the Callable can't be 
	 * immediately determined. like its synchronous version, the given task will repeat as long as it
	 * returns true and will stop repeating once it returns false, except here results are retrieved via
	 * CompletableFutures. note that in this case, the delay until the next iteration of the task begins 
	 * won't start counting down until the CompletableFuture from the previous iteration is completed
	 * <p>
	 * note that while this executor will take on some overhead to make this method work, the passed in
	 * CompletableFuture will still execute in whatever executor it was designated with on creation. ie,
	 * if the passed in Callable has 
	 * <pre>
	 * return CompletableFuture.supplyAsync(() -> { ... }, someOtherExecutor);</pre> 
	 * then each iteration of the given action will still cause the given CompletableFuture to execute 
	 * in <code>someOtherExecutor</code>, rather than this executor
	 * @param action the task to execute. it will continue repeating as long as its returned 
	 * CompletableFuture continues to complete with true, and will stop repeating once its returned
	 * CompletableFuture completes with false
	 * @param initialDelay the delay until the first execution of the given task
	 * @param delay the delay between when one iteration's CompletableFuture completes and when the
	 * next iteration begins
	 * @param unit the unit of the given delay
	 */
	public void scheduleWithFixedDelayAndConditionAsync(Callable<CompletableFuture<Boolean>> action, long initialDelay,
			long delay, TimeUnit unit) {
		this.inner.schedule(this.scheduledConditionalAsyncWrapper(action, delay, unit), initialDelay, unit);
	}
	
	private Runnable scheduledConditionalAsyncWrapper(final Callable<CompletableFuture<Boolean>> action,
			long delay, TimeUnit unit) {
		return (() -> {
			CompletableFuture<Boolean> future;
			try {
				future = action.call();
			} catch (Exception e) {
				logger.warn("exception thrown in scheduled repeating conditional", e);
				return;
			}
			future.whenCompleteAsync((result, throwable) -> {
				if(throwable != null) {
					logger.warn("exception thrown in scheduled repeating conditional", throwable);
				} else if (result) {
					this.inner.schedule(this.scheduledConditionalAsyncWrapper(action, delay, unit), delay, unit);
				}
			}, this.inner);
		});
	}

}
