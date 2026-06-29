import java.util.Iterator;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.type.numeric.real.DoubleType;

public class NgffOneOffs {

	public static void main(String[] args) {

//		readAffine()
		readCoordinates();
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
