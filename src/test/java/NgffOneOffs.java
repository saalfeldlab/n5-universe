import java.util.Iterator;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;

import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.type.numeric.real.DoubleType;

public class NgffOneOffs {

	public static void main(String[] args) {

//		readAffine()
//		readCoordinates();
//		jrc18FcwbDemo();
		miaData();

		// get reader
//		N5Reader zarr = new N5Factory().openReader("https://radosgw.public.os.wwu.de/rfc5-transform-test-data/logan_shepp_rotation_30_clockwise.ome.zarr");
//		System.out.println(zarr.getClass().getName());
		
	}

	public static void jrc18FcwbDemo() {

		N5Reader zarr = new N5Factory().openReader("/home/john/data/jrc18_demo_sample_data/JRC2018F_FCWB_small.ome.zarr");
		System.out.println(zarr.getClass().getName());

		N5TreeNode node = N5DatasetDiscoverer.discover(zarr);
		System.out.println( node.getMetadata().getClass().getName());
	}
	
	public static void miaData() {

		N5Reader zarr = new N5Factory().openReader("/home/john/data/ome-zarr/v0.5/em-mouse-MICrONS-minnie65/crop-001.zarr");
		System.out.println(zarr.getClass().getName());

		N5TreeNode node = N5DatasetDiscoverer.discover(zarr);

		final N5Metadata meta = node.getMetadata();
		System.out.println( meta.getClass().getName());
	}

	public static void readCoordinates() {

//		String uri = "/home/john/dev/ngff/ome_zarr_transformations_conformance/cases/coordinates_1d.ome.zarr/coordinateTransformations/inputToOutput";
		String uri = "/home/john/dev/ngff/ome_zarr_transformations_conformance/cases/coordinates_2d-3d.ome.zarr/coordinateTransformations/inputToOutput"; try ( final N5Reader n5 = new N5Factory().openReader(uri) ) {
			
			System.out.println( n5.datasetExists(""));
			CachedCellImg<DoubleType, ?> mtx = N5Utils.open(n5, "");
			Iterator<DoubleType> it = mtx.iterator();
			while( it.hasNext())
			{
				System.out.println(it.next());
			}

		}
	}

	public static void readAffine() {

		String uri = "/home/john/data/ngff/ngff-rfc5-coordinate-transformation-examples/3d/simple/affineParams.zarr/affineParams";
		try ( final N5Reader n5 = new N5Factory().openReader(uri) ) {
			
			System.out.println( n5.datasetExists(""));
			CachedCellImg<DoubleType, ?> mtx = N5Utils.open(n5, "");
			Iterator<DoubleType> it = mtx.iterator();
			while( it.hasNext())
			{
				System.out.println(it.next());
			}
		}
	}

}
