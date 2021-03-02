/**
 *
 */
package org.janelia.saalfeldlab.n5.universe;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.imglib2.loops.LoopBuilder.TriConsumer;

/**
 * @author Stephan Saalfeld
 *
 */
public class ASync {

	public static <T> void async(
			final Supplier<T> request,
			final Consumer<T> callback) {

		new Thread(() -> callback.accept(request.get())).run();
	}

	public static <T, E extends Exception> void async(
			final Supplier<T> request,
			final Consumer<T> callback,
			final TriConsumer<Supplier<T>, Consumer<T>, Exception> except) {

		new Thread(() -> {
			try {
				callback.accept(request.get());
			} catch (final Exception e) {
				except.accept(request, callback, e);
			}
		});
	}

	public static <T> void async(
			final Supplier<T> request,
			final Consumer<T> callback,
			final ExecutorService exec) {

		exec.submit(() -> callback.accept(request.get()));
	}

	public static <T, E extends Exception> void async(
			final Supplier<T> request,
			final Consumer<T> callback,
			final TriConsumer<Supplier<T>, Consumer<T>, Exception> except,
			final ExecutorService exec) {

		exec.submit(() -> {
			try {
				callback.accept(request.get());
			} catch (final Exception e) {
				except.accept(request, callback, e);
			}
		});
	}
}
