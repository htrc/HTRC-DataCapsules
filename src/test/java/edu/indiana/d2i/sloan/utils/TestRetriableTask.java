/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import edu.indiana.d2i.sloan.utils.RetriableTask;

public class TestRetriableTask {
	
	@Test(expected = RuntimeException.class)
	public void testRetriable() throws Exception {
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					throw new RuntimeException("retry exception.");
				}
			}, 500, 1);
		r.call();
	}
	
	@Test(expected = java.util.concurrent.TimeoutException.class)
	public void testThreadException() throws Exception {		
		RetriableTask<Void> r = new RetriableTask<Void>(
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					ExecutorService executor = null;
					try {
						executor = Executors.newSingleThreadExecutor();
						Future<Void> future = executor.submit(
							new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									Thread.sleep(2000);
									return null;
								}
							} );
						return future.get(500, TimeUnit.MILLISECONDS);
					} finally {
						if (executor != null)
							executor.shutdownNow();
					}
				}
			},  1000, 2);
		r.call();
	}
}
