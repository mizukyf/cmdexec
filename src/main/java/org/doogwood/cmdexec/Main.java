package org.doogwood.cmdexec;

import java.util.Arrays;

import org.doogwood.cmdexec.ExternalCommand.Result;

public final class Main {
	public static void main(final String[] args) throws Exception {
		executeAndPrintLines("ls -la");
		executeAndPrintLines("ls", "-la");
		executeAndPrintLines("ls", "-la", "no_such_file.txt");
		executeAndPrintLines("ping -c 2 localhost");
		executeAndPrintLines("ping -c 5 localhost");
	}
	
	private static void executeAndPrintLines(final String... commandAndArgs) {
		System.out.println("Execute: " + Arrays.asList(commandAndArgs));
		// コマンドをパースしてオブジェクト化
		final ExternalCommand cmd = ExternalCommand.parse(commandAndArgs);
		// タイムアウト時間に3000ミリ秒を指定しつつ実行
		final Result res = cmd.execute(3000);
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
}
