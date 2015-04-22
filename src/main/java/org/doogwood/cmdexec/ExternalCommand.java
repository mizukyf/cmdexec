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

/**
 * 外部コマンドを表わすオブジェクト.
 * 同期もしくは非同期で当該コマンドを実行するためのメソッドを提供する。
 */
public final class ExternalCommand {
	/**
	 * Apache Commons Execのコマンドライン・オブジェクト.
	 */
	private final CommandLine commandLine;
	/**
	 * コンストラクタ.
	 * 静的メソッドを介した初期化のみ許可する。
	 */
	private ExternalCommand(final String commandLine) {
		this.commandLine = CommandLine.parse(commandLine);
	}
	/**
	 * Apache Commons Execのコマンドライン・オブジェクトを返す.
	 * @return コマンドライン・オブジェクト
	 */
	public CommandLine getCommandLine() {
		return commandLine;
	}
	/**
	 * タイムアウト指定なしで同期実行する.
	 * @return 実行結果
	 */
	public Result execute() {
		return execute(0);
	}
	/**
	 * タイムアウト指定ありで同期実行する.
	 * @param timeoutMillis 実行を打ち切るまでのミリ秒
	 * @return 実行結果
	 */
	public Result execute(final long timeoutMillis) {
		// 標準出力を受け取るためのストリームを初期化
		final PipeOutputStream out = new PipeOutputStream();
		// 標準エラーを受け取るためのストリームを初期化
		final PipeOutputStream err = new PipeOutputStream();
		// ストリームを引数にしてストリームハンドラを初期化
		final PumpStreamHandler streamHandler = new PumpStreamHandler(out, err);
		// エグゼキュータを初期化
		final Executor exec = new DefaultExecutor();
		// タイムアウト指定の引数を確認
		if (timeoutMillis > 0) {
			// 1以上の場合のみ実際にエグゼキュータに対して設定を行う
			exec.setWatchdog(new ExecuteWatchdog(timeoutMillis));
		}
		// 終了コードによるエラー判定をスキップするよう指定
		exec.setExitValues(null);
		// ストリームハンドラを設定
		exec.setStreamHandler(streamHandler);
		
		try {
			// 実行して終了コードを受け取る（同期実行する）
			final int exitCode = exec.execute(commandLine);
			// 実行結果を呼び出し元に返す
			return new Result(exitCode, out.getInputStream(), err.getInputStream());
		} catch (final ExecuteException e) {
			// 終了コード判定はスキップされるためこの例外がスローされるのは予期せぬ事態のみ
			// よって非チェック例外でラップして再スローする
			throw new RuntimeException(e);
		} catch (final IOException e) {
			// IOエラーの発生は予期せぬ事態
			// よって非チェック例外でラップして再スローする
			throw new RuntimeException(e);
		}
	}
	/**
	 * タイムアウト指定なしで非同期実行する.
	 * @return 実行結果にアクセスするためのFutureオブジェクト
	 */
	public Future<Result> executeAsynchronously() {
		return executeAsynchronously(0);
	}
	/**
	 * タイムアウト指定ありで非同期実行する.
	 * @param timeoutMillis 実行を打ち切るまでのミリ秒
	 * @return 実行結果にアクセスするためのFutureオブジェクト
	 */
	public Future<Result> executeAsynchronously(final long timeoutMillis) {
		final ExecutorService service = Executors.newSingleThreadExecutor();
		return service.submit(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return ExternalCommand.this.execute(timeoutMillis);
			}
		});
	}
	/**
	 * 外部コマンド文字列を受け取りオブジェクトを初期化する.
	 * @param commandLine 外部コマンド文字列
	 * @return オブジェクト
	 */
	public static ExternalCommand parse(final String commandLine) {
		return new ExternalCommand(commandLine);
	}
	/**
	 * 実行結果を表わすオブジェクト.
	 */
	public static final class Result {
		/**
		 * 終了コード.
		 */
		private final int exitCode;
		/**
		 * 標準出力の内容にアクセスするための{@link InputStream}.
		 */
		private final InputStream inputFromStdout;
		/**
		 * 標準エラーの内容にアクセスするための{@link InputStream}.
		 */
		private final InputStream inputFromStderr;
		/**
		 * コンストラクタ.
		 * @param exitCode 終了コード
		 * @param inputFromStdout 標準出力の内容にアクセスするための{@link InputStream}
		 * @param inputFromStderr 標準エラーの内容にアクセスするための{@link InputStream}
		 */
		private Result(final int exitCode, final InputStream inputFromStdout, final InputStream inputFromStderr) {
			this.exitCode = exitCode;
			this.inputFromStdout = inputFromStdout;
			this.inputFromStderr = inputFromStderr;
		}
		/**
		 * 終了コードを返す.
		 * @return 終了コード
		 */
		public int getExitCode() {
			return exitCode;
		}
		/**
		 * 標準出力の内容にアクセスするための{@link InputStream}を返す.
		 * @return {@link InputStream}
		 */
		public InputStream getStdout() {
			return inputFromStdout;
		}
		/**
		 * 標準エラーの内容にアクセスするための{@link InputStream}を返す.
		 * @return {@link InputStream}
		 */
		public InputStream getStderr() {
			return inputFromStderr;
		}
		/**
		 * 標準出力の内容に行ごとにアクセスするための{@link Iterable}を返す.
		 * @return {@link Iterable}
		 */
		public Iterable<String> getStdoutLines() {
			return readLines(inputFromStdout);
		}
		/**
		 * 標準エラーの内容に行ごとにアクセスするための{@link Iterable}を返す.
		 * @return {@link Iterable}
		 */
		public Iterable<String> getStderrLines() {
			return readLines(inputFromStderr);
		}
		/**
		 * ストリームから文字列を読み出し行ごとのリストに変換する.
		 * @param in ストリーム
		 * @return リスト
		 */
		private List<String> readLines(final InputStream in) {
			final BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()));
			final List<String> result = new ArrayList<String>();
			String line = null;
			try {
				while ((line = br.readLine()) != null) {
					result.add(line);
				}
			} catch (final IOException e) {
				// 読み取り対象のストリームはByteArrayInputStreamである前提のため
				// IOエラーが起きることは実際上あり得ないこと
				// 万一エラーが起きた場合でも非チェック例外で包んで再スローする
				throw new RuntimeException(e);
			}
			return result;
		}
	}
}
