package org.doogwood.cmdexec;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;

public class PipeOutputStreamTest {
	
	private static PipeOutputStream makeStream(final int threshold) {
		return new PipeOutputStream(threshold);
	}
	
	private static PipeOutputStream makeStreamThenWriteData(final int threshold, final byte[] data) throws IOException {
		final PipeOutputStream s = makeStream(threshold);
		s.write(data);
		s.close();
		return s;
	}

	@Test
	public void constructorTest00() throws IOException {
		final PipeOutputStream out = makeStreamThenWriteData(0, "0123456789".getBytes());
		final BufferedReader br = new BufferedReader(new InputStreamReader(out.getInputStream()));
		final String line = br.readLine();
		assertThat(line, is("0123456789"));
	}

	@Test
	public void constructorTest01() throws IOException {
		final PipeOutputStream out = makeStreamThenWriteData(0, "0123456789\n9876543210".getBytes());
		final BufferedReader br = new BufferedReader(new InputStreamReader(out.getInputStream()));
		assertThat(br.readLine(), is("0123456789"));
		assertThat(br.readLine(), is("9876543210"));
	}

	@Test
	public void constructorTest02() throws IOException {
		final PipeOutputStream out = makeStreamThenWriteData(10, "0123456789\n9876543210".getBytes());
		final BufferedReader br = new BufferedReader(new InputStreamReader(out.getInputStream()));
		assertThat(br.readLine(), is("0123456789"));
		assertThat(br.readLine(), is("9876543210"));
	}

	@Test
	public void constructorTest03() throws IOException {
		try {
			final PipeOutputStream out = makeStream(-1);
			System.out.println(out.toString());
			fail();
		} catch (final IllegalArgumentException e) {
			// Ok.
		}
	}

	@Test
	public void isReadyForReadingTest00() throws IOException {
		final PipeOutputStream out = makeStream(0);
		assertThat(out.isReadyForReading(), is(false));
		out.write("0123456789\n".getBytes());
		assertThat(out.isReadyForReading(), is(false));
		out.write("9876543210".getBytes());
		assertThat(out.isReadyForReading(), is(false));
		out.close();
		assertThat(out.isReadyForReading(), is(true));
		final BufferedReader br = new BufferedReader(new InputStreamReader(out.getInputStream()));
		assertThat(br.readLine(), is("0123456789"));
		assertThat(br.readLine(), is("9876543210"));
	}

	@Test
	public void isReadyForReadingTest01() throws IOException {
		final PipeOutputStream out = makeStream(0);
		assertThat(out.isReadyForReading(), is(false));
		out.write("0123456789\n".getBytes());
		assertThat(out.isReadyForReading(), is(false));
		out.write("\n".getBytes());
		assertThat(out.isReadyForReading(), is(false));
		out.write("9876543210".getBytes());
		assertThat(out.isReadyForReading(), is(false));
		
		try {
			final InputStream in = out.getInputStream();
			System.out.println(in.toString());
			fail();
		} catch (final IllegalStateException e) {
			// Ok.
		}
		
		out.close();
		assertThat(out.isReadyForReading(), is(true));
	}

	@Test
	public void isUsingTempFileTest00() throws IOException {
		final PipeOutputStream out = makeStream(10);
		assertThat(out.isUsingTempFile(), is(false));
		out.write("0123456789".getBytes());
		assertThat(out.isUsingTempFile(), is(false));
		out.write("\n".getBytes());
		assertThat(out.isUsingTempFile(), is(true));
		out.write("9876543210".getBytes());
		assertThat(out.isUsingTempFile(), is(true));
		out.close();
		final BufferedReader br = new BufferedReader(new InputStreamReader(out.getInputStream()));
		assertThat(br.readLine(), is("0123456789"));
		assertThat(br.readLine(), is("9876543210"));
	}
}
