package org.janelia.saalfeldlab.n5.universe.translation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.TransformTests;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;


public class TranslatedN5Tests {

	private File containerDir;
	private N5FSWriter n5;
	private ArrayImg<UnsignedByteType, ByteArray> img;

	@Before
	public void before()
	{
		URL configUrl = TransformTests.class.getResource( "/n5.jq" );
		File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "n5Translation.n5" );

		try {
			n5 = new N5FSWriter( containerDir.getCanonicalPath() );
			
			Random rand = new Random( 945 );
			img = ArrayImgs.unsignedBytes(3, 4, 5);
			ArrayCursor<UnsignedByteType> c = img.cursor();
			while( c.hasNext())
				c.next().set( rand.nextInt() );

			N5Utils.save( img, n5, "img", new int[] {3,4,5}, new RawCompression());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@After
	public void after() {
		try {
			n5.remove();
		} catch (IOException e) { }
	}
	
	@Test
	public void testPathTranslation() {

		try {
			n5.createGroup("/pathXlation");
			n5.createDataset("/pathXlation/src", 
					new DatasetAttributes( new long[]{16,16}, new int[]{16,16}, DataType.UINT8, new RawCompression()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertTrue("pathXlation src exists", n5.exists("/pathXlation/src"));

//		final String translation = "include \"n5\"; moveSubTree( \"/pathXlation/src\"; \"/pathXlation/dst\" )";
//		final TranslatedN5Reader n5Xlated = new TranslatedN5Reader(n5, translation, ".");
//		System.out.println( n5Xlated.exists("pathXlation/dst"));


		// move "img" dataset to "data"
		final String fwdTranslation = "include \"n5\"; moveSubTree( \"/img\"; \"data\" )";
		final String invTranslation = "include \"n5\"; moveSubTree( \"/data\"; \"img\" )";
		final TranslatedN5Reader n5Xlated = new TranslatedN5Reader(n5, fwdTranslation, invTranslation );
		Assert.assertTrue("translated dataset exists", n5Xlated.exists("data"));

		DatasetAttributes attrs;
		try {
			attrs = n5Xlated.getDatasetAttributes("data");
			Assert.assertNotNull("translated dataset attributes exist", attrs);

			CachedCellImg<UnsignedByteType, ?> imgFromXlated = N5Utils.open(n5Xlated, "data");
			System.out.println( Intervals.toString(imgFromXlated));
			Assert.assertNotNull("translated img readable", attrs);
			
			boolean imgsAreEqual = equal(img, imgFromXlated);
			Assert.assertTrue("translated img correct", imgsAreEqual );

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static < T extends RealType< T > & NativeType< T > > boolean equal( final Img<T> imgA, final Img<T> imgB ) {
		try {
			final Cursor< T > c = imgA.cursor();
			final RandomAccess< T > r = imgB.randomAccess();
			while( c.hasNext() )
			{
				c.fwd();
				r.setPosition( c );
				if( c.get().getRealDouble() != r.get().getRealDouble() )
					return false;
			}
			return true;
		}catch( final Exception e )
		{
			return false;
		}
	}	


}
