package org.doogwood.cmdexec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
	 * データを貯めこむバイト配列ストリーム.
	 */
	private final ByteArrayOutputStream inner = new ByteArrayOutputStream();

	@Override
	public void write(final int b) throws IOException {
		inner.write(b);
	}
	
	/**
	 * 入力ストリームを生成して返す.
	 * @return 入力ストリーム
	 */
	public InputStream getInputStream() {
		return new ByteArrayInputStream(inner.toByteArray());
	}
}
