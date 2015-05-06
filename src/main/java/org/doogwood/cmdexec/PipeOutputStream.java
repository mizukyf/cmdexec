package org.doogwood.cmdexec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
		byteCount ++;
		if (byteCount <= threshold) {
			writeIntoByteArray(b);
		} else {
			if (tempFile == null) {
				makeTempFile();
			}
			writeIntoTempFile(b);
		}
	}
	
	private void makeTempFile() throws IOException {
		tempFile = File.createTempFile("pipeOutputStream", ".tmp");
		tempFile.deleteOnExit();
		tempFileOutputStream = new FileOutputStream(tempFile);
		tempFileOutputStream.write(byteArrayOutputStream.toByteArray());
		byteArrayOutputStream = null;
	}
	
	private void writeIntoTempFile(final int b) throws IOException {
		tempFileOutputStream.write(b);
	}
	
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
		if (tempFileOutputStream != null) {
			tempFileOutputStream.close();
			tempFileOutputStream = null;
		}
		closed = true;
	}
	
	/**
	 * 入力ストリームを生成して返す.
	 * @return 入力ストリーム
	 * @throws IOException 
	 */
	public InputStream getInputStream() throws IOException {
		if (!isReadyForReading()) {
			throw new IllegalStateException();
		}
		if (tempFile == null) {
			return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		} else {
			return new FileInputStream(tempFile);
		}
	}
}
