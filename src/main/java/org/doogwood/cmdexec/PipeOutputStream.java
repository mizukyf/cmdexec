package org.doogwood.cmdexec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 入力ストリームを生成する出力ストリーム.
 * 出力ストリームとして何かしらの処理の結果を受け取り内部的に貯めこんで、
 * その結果をもとにして入力ストリームを生成する。
 */
public final class PipeOutputStream extends OutputStream {
	/**
	 * 一時ファイル作成を判断する閾値.
	 */
	private final int threshold;
	/**
	 * これまでに書き込んだバイト数.
	 */
	private int byteCount = 0;
	/**
	 * データを貯めこむバイト配列ストリーム.
	 */
	private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	/**
	 * データを貯めこむ一時ファイル.
	 */
	private File tempFile = null;
	/**
	 * 一時ファイルのための出力ストリーム.
	 */
	private FileOutputStream tempFileOutputStream = null;
	/**
	 * ストリームへの書き込みが終わっているかどうかを示す.
	 */
	private boolean closed = false;
	
	/**
	 * コンストラクタ.
	 * @param threshold 一時ファイル作成を判断する閾値（単位はバイト）
	 */
	public PipeOutputStream(final int threshold) {
		if (threshold < 0) {
			throw new IllegalArgumentException();
		}
		this.threshold = threshold;
	}
	/**
	 * コンストラクタ.
	 * 一時ファイル作成を判断する閾値には1MB（{@code 1028 * 1028}バイト）が設定される。
	 */
	public PipeOutputStream() {
		this(1028 * 1028);
	}
	
	@Override
	public void write(final int b) throws IOException {
		// 閾値をチェック
		if (byteCount < threshold) {
			// 閾値未満である場合

			// 書き込みバイト数をインクリメント
			byteCount ++;
			// バイト配列への書き込み処理を実施
			writeIntoByteArray(b);
			
		} else {
			// すでに閾値に到達している場合
			
			// 一時ファイルインスタンスへの参照をチェック
			if (tempFile == null) {
				// なければつくる
				makeTempFile();
			}
			// 一時ファイルへの書き込み処理を実施
			writeIntoTempFile(b);
		}
	}
	/**
	 * 一時ファイルを作成する.
	 * すでにバイト配列に書き込み済みのデータがあればこれを一時ファイルに書き出す。
	 * @throws IOException 一時ファイル作成とデータ書き込みの最中にエラーが発生した場合
	 */
	private void makeTempFile() throws IOException {
		// 一時ファイル・インスタンスを生成
		tempFile = File.createTempFile("pipeOutputStream", ".tmp");
		// JVMが終了するとき一時ファイルも削除するよう指示
		tempFile.deleteOnExit();
		// 一時ファイルへの書き込みようにストリームを生成
		tempFileOutputStream = new FileOutputStream(tempFile);
		// すでにバイト配列に書き込んでいたデータを移し替え
		tempFileOutputStream.write(byteArrayOutputStream.toByteArray());
		// ByteArrayOutputStreamのインスタンスへの参照を破棄
		byteArrayOutputStream = null;
	}
	/**
	 * 一時ファイルに対してデータを書き込む.
	 * @param b データ
	 * @throws IOException データ書き込み中にエラーが発生した場合
	 */
	private void writeIntoTempFile(final int b) throws IOException {
		tempFileOutputStream.write(b);
	}
	/**
	 * バイト配列に対してデータを書き込む.
	 * @param b データ
	 */
	private void writeIntoByteArray(final int b) {
		byteArrayOutputStream.write(b);
	}
	/**
	 * 一時ファイルを使用している場合{@code true}を返す.
	 * @return 判定結果
	 */
	public boolean isUsingTempFile() {
		return tempFile != null;
	}
	/**
	 * データを読み込む準備ができている場合{@code true}を返す.
	 * このメソッドが{@code true}を返した場合、
	 * この{@code OutputStream}へのデータの書き出しが完了しており、
	 * {@link #getInputStream()}を呼び出す準備ができていることを示す。
	 * @return 判定結果
	 */
	public boolean isReadyForReading() {
		return closed;
	}
	@Override
	public final void close() throws IOException {
		try {
			// 一時ファイルのFileOutputStreamへの参照をチェック
			if (tempFileOutputStream != null) {
				// もし参照があればともかくclose()を呼び出す
				tempFileOutputStream.close();
			}
		} finally {
			// FileOutputStreamの事後処理の結果にかかわらず
			// 当該インスタンスへの参照は破棄
			tempFileOutputStream = null;
			// PipeOutputStreamとしてはクローズ済みとしてマークする
			closed = true;
		}
	}
	/**
	 * 入力ストリームを生成して返す.
	 * @return 入力ストリーム
	 */
	public InputStream getInputStream() {
		// 読み込み準備ができているかチェック
		if (!isReadyForReading()) {
			// できていない場合は実行時例外をスロー
			throw new IllegalStateException();
		}
		// 一時ファイルの有無をチェック
		if (tempFile == null) {
			// 一時ファイルがない＝バイト配列でデータを保持している
			// バイト配列からByteArrayInputStreamを生成して返す
			return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		} else {
			try {
				// 一時ファイルからFileInputStreamを生成して返す
				return new FileInputStream(tempFile);
			} catch (final FileNotFoundException e) {
				// 一時ファイルが見つからない＝予期せぬエラー
				// 実行時例外をスローする
				throw new IllegalStateException(e);
			}
		}
	}
}
