package org.doogwood.cmdexec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

public final class Command {
	private final CommandLine commandLine;
	private Command(final String commandLine) {
		this.commandLine = CommandLine.parse(commandLine);
	}
	
	public CommandLine getCommandLine() {
		return commandLine;
	}
	
	public Result execute() {
		return execute(0);
	}
	
	public Result execute(final long timeoutMilis) {
		// コマンド実行結果を受け取るためのストリームを初期化
		final PipeOutputStream out = new PipeOutputStream();
		final PipeOutputStream err = new PipeOutputStream();
		// ストリームを引数にしてストリームハンドラを初期化
		final PumpStreamHandler streamHandler = new PumpStreamHandler(out, err);
		// エグゼキュータを初期化
		final Executor exec = new DefaultExecutor();
		if (timeoutMilis > 0) {
			exec.setWatchdog(new ExecuteWatchdog(timeoutMilis));
		}
		// 終了コードによるエラー判定をOFF
		exec.setExitValues(null);
		// ストリームハンドラを設定
		exec.setStreamHandler(streamHandler);
		// 実行して終了コードを受け取る（同期実行する）
		try {
			final int exitCode = exec.execute(commandLine);
			return new Result(exitCode, out.getInputStream(), err.getInputStream());
		} catch (final ExecuteException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Future<Result> executeAsynchronously() {
		final ExecutorService service = Executors.newSingleThreadExecutor();
		return service.submit(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return Command.this.execute();
			}
		});
	}
	
	public static Command parse(final String commandLine) {
		return new Command(commandLine);
	}
	
	public static final class Result {
		private final int exitCode;
		private final InputStream inputFromStdout;
		private final InputStream inputFromStderr;
		private Result(final int exitCode, final InputStream inputFromStdout, final InputStream inputFromStderr) {
			this.exitCode = exitCode;
			this.inputFromStdout = inputFromStdout;
			this.inputFromStderr = inputFromStderr;
		}
		public int getExitCode() {
			return exitCode;
		}
		public InputStream getStdout() {
			return inputFromStdout;
		}
		public InputStream getStderr() {
			return inputFromStderr;
		}
		public Iterable<String> getStdoutLines() {
			return readLines(inputFromStdout);
		}
		public Iterable<String> getStderrLines() {
			return readLines(inputFromStderr);
		}
		private List<String> readLines(final InputStream in) {
			final BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()));
			final List<String> result = new ArrayList<String>();
			String line = null;
			try {
				while ((line = br.readLine()) != null) {
					result.add(line);
				}
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
			return result;
		}
		
	}
}
