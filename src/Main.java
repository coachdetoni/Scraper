import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        Document doc = null;
        try {
            doc = Jsoup.connect("http://tvtotal.prosieben.de/frontend/php/ajax.php?length=1024&query=query%3DgetRelatedVideosByNormalizedTags%26tags%3Dnippel%26length%3D12%26offset%3D0").get();
        } catch (IOException e) {
            System.err.println("Could not connect to TVTotal");
            System.exit(-1);
        }
        Elements nippelDivs = doc.getElementsByClass("item");
        for(Element nippelElement : nippelDivs) {
            try {
                downloadElement(nippelElement);
            } catch (IOException e) {
                System.out.println("_");
            }
        }

        System.out.println("Done!");
    }

    private static void downloadElement(Element nippelElement) throws IOException {
        if (!nippelElement.selectFirst("span.length").text().startsWith("00:"))
            return;

        String link = nippelElement.select("a").first().attr("abs:href");
        String title = nippelElement.select("h4").first().text();

        String name = title.replaceAll("[:\\\\/*?|<>]", "_") + ".mp4";
        File existingFile = new File(name);
        if(existingFile.exists()){
            System.out.println("Skipped " + name + " because it already exists");
            return;
        }

        Document nipSource = Jsoup.connect(link).get();

        for (Element scripts : nipSource.getElementsByTag("script")) {
            for (DataNode dataNode : scripts.dataNodes()) {
                String data = dataNode.getWholeData();
                if (data.contains("videoURL")) {
                    Pattern p = Pattern.compile("videoURL: '(.+)'");
                    Matcher m = p.matcher(data);
                    while (m.find()) {
                        String vidLink = m.group(1);

                        System.out.println("Downloading " + name + " " + vidLink);

                        URL website = new URL(vidLink);
                        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                        FileOutputStream fos = new FileOutputStream(name);
                        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    }
                }
            }
        }
    }
}
