package org.doogwood.cmdexec;

import static org.junit.Assert.*;

import org.apache.commons.lang3.SystemUtils;
import org.doogwood.cmdexec.ExternalCommand.Result;

import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

public class ExternalCommandTest {
	
	private String okLsCommand() {
		if (SystemUtils.IS_OS_MAC_OSX) {
			return "ls -la";
		} else if (SystemUtils.IS_OS_WINDOWS) {
			return "dir";
		} else {
			throw new RuntimeException();
		}
	}
	
	private String ngLsCommand() {
		if (SystemUtils.IS_OS_MAC_OSX) {
			return "ls -la no_such_file.txt";
		} else if (SystemUtils.IS_OS_WINDOWS) {
			return "dir no_such_file.txt";
		} else {
			throw new RuntimeException();
		}
	}
	
	private String okPing5TimesCommand() {
		if (SystemUtils.IS_OS_MAC_OSX) {
			return "ping -c 5 localhost";
		} else if (SystemUtils.IS_OS_WINDOWS) {
			return "ping -n 5 localhost";
		} else {
			throw new RuntimeException();
		}
	}
	
	private String ngCpCommand() {
		if (SystemUtils.IS_OS_MAC_OSX) {
			return "cp";
		} else if (SystemUtils.IS_OS_WINDOWS) {
			return "copy";
		} else {
			throw new RuntimeException();
		}
	}
	
	private void printResult(final String label, final String cmd, final Result res) {
		System.out.println("[" + label + "]");
		System.out.println("CommandLine: " + cmd);
		// 終了コードを確認する
		System.out.println("ExitCode: " + res.getExitCode());
		// コマンドの標準出力の内容から入力ストリームを生成してそこから再度内容を読み取る
		System.out.println("Stdout: ");
		for (final String line : res.getStdoutLines()) {
			System.out.println("1>  " + line);
		}
		// コマンドの標準エラーの内容から入力ストリームを生成してそこから再度内容を読み取る
		System.out.println("Stderr: ");
		for (final String line : res.getStderrLines()) {
			System.out.println("2>  " + line);
		}
		System.out.println();
	}
	
	@Test
	public void executeTest00() {
		final String cmd = okLsCommand();
		final Result res = ExternalCommand.parse(cmd).execute();
		printResult("executeTest00", cmd, res);
		assertThat(res.getExitCode(), is(0));
		assertThat(res.getStdoutLines().iterator().hasNext(), is(true));
		assertThat(res.getStderrLines().iterator().hasNext(), is(false));
	}

	@Test
	public void executeTest01() {
		final String cmd = ngLsCommand();
		final Result res = ExternalCommand.parse(cmd).execute();
		printResult("executeTest01", cmd, res);
		assertThat(res.getExitCode(), is(1));
		assertThat(res.getStdoutLines().iterator().hasNext(), is(false));
		assertThat(res.getStderrLines().iterator().hasNext(), is(true));
	}

	@Test
	public void executeTest02() {
		final String cmd = okPing5TimesCommand();
		final Result res = ExternalCommand.parse(cmd).execute();
		printResult("executeTest02", cmd, res);
		assertThat(res.getExitCode(), is(0));
		assertThat(res.getStdoutLines().iterator().hasNext(), is(true));
		assertThat(res.getStderrLines().iterator().hasNext(), is(false));
	}

	@Test
	public void executeTest03() {
		final String cmd = okPing5TimesCommand();
		final Result res = ExternalCommand.parse(cmd).execute(1000);
		printResult("executeTest03", cmd, res);
		assertNotEquals(res.getExitCode(), 0);
		assertThat(res.getStdoutLines().iterator().hasNext(), is(true));
		assertThat(res.getStderrLines().iterator().hasNext(), is(false));
	}

	@Test
	public void executeTest04() {
		final String cmd = ngCpCommand();
		final Result res = ExternalCommand.parse(cmd).execute();
		printResult("executeTest04", cmd, res);
		assertNotEquals(res.getExitCode(), 0);
		assertThat(res.getStdoutLines().iterator().hasNext(), is(false));
		assertThat(res.getStderrLines().iterator().hasNext(), is(true));
	}
}
