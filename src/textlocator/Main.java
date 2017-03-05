package textlocator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {

	public static void main(String[] argv) {
		BufferedImage rgbImg = null;
		try {
			rgbImg = ImageIO.read(new File(argv[0]));
			System.out.println(TextPositionDetector.apply(rgbImg));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
