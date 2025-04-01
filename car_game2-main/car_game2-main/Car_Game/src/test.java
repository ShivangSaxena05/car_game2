import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

public class test {
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture cap = new VideoCapture(0);
		Mat mat = new Mat();
		cap.read(mat);
		String xmlfil = "C:/Users/hp/Downloads/opencv/sources/data/lbpcascades/lbpcascade_frontalcatface.xml";
		CascadeClassifier clss = new CascadeClassifier(xmlfil);
		MatOfRect face = new MatOfRect();
		clss.detectMultiScale(mat, face);
		System.out.println(String.format("Detected %s faces", face.toArray().length));
	}
}
