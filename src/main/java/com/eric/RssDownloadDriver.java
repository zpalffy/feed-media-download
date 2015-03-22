package com.eric;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v2.APICID3V2Frame;
import org.blinkenlights.jid3.v2.ID3V2Tag;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;
import org.blinkenlights.jid3.v2.APICID3V2Frame.PictureType;

import com.beust.jcommander.Parameter;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RssDownloadDriver extends CommandRunner {

    // TODO fill in readme.txt

    private static final String M3U_FILE_NAME = "all.m3u";

    /**
     * The mp3 file extension.
     */
    private static final String MP3_EXTENSION = "mp3";

    /**
     * The extensions that are considered to be media files. These should be lowercase here.
     */
    private static final String[] MEDIA_EXTENSIONS = {MP3_EXTENSION, "avi", "mp4", "mov"};

    /**
     * Filter for filtering by media extension.
     */
    private static final FileFilter EXTENSION_FILTER = new SuffixFileFilter(MEDIA_EXTENSIONS);

    /**
     * The list of rss urls. Usually this is of length one since these all go under the same
     * directory.
     */
    // TODO bug here if more than one specified since last mod times are not combined.
    @Parameter(description = "<feed-url>", required=true)
    private List<String> rssUrls;

    @Parameter(names = {"-v", "--verbose"}, description = "Display more output.")
    private boolean verbose;

    @Parameter(names = {"-e", "--max-episodes"}, description = "The maximum number of episodes to download and keep.  Setting this to 0 will keep all episodes.")
    private int episodesToKeep = 3;

    @Parameter(names = {"-k", "--keep-file-name"}, description = "If set, the original file name from the enclosure url is used.  If left unset, the entry title is used instead.")
    private boolean keepFileName;

    @Parameter(names = {"-d", "--directory"}, description = "The directory to download episodes to and remove from if max is reached.  If not specified then the current directory is used.")
    private String directory = System.getProperty("user.dir");

    @Parameter(names = "-artist", description = "Always sets the artist ID3 tag to this value if the file is an mp3.")
    private String artist;

    @Parameter(names = "-album", description = "Always sets the album ID3 tag to this value if the file is an mp3.")
    private String album;

    @Parameter(names = "-comment", description = "Always sets the comment ID3 tag to this value if the file is an mp3.")
    private String comment;

    @Parameter(names = "-genre", description = "Always sets the genre ID3 tag to this value if the file is an mp3.")
    private String genre;

    @Parameter(names = "-title", description = "Always sets the title ID3 tag to this value if the file is an mp3.  Note this does not affect the file name.")
    private String title;

    @Parameter(names = "-year", description = "Always sets the year ID3 tag to this value if the file is an mp3.")
    private Integer year;

    @Parameter(names = "-art", description = "Always sets the cover art to the image from this url if the file is an mp3.")
    private String artUrl;

    @Parameter(names = {"-t", "--tags-from-feed"}, description = "If set and the file is an mp3, ID3 tags will be set on the file matching the values from the feed.")
    private boolean readTagsFromFeed;

    @Parameter(names = {"-m", "--m3u"}, description = "Generate an m3u file in the podcast directory with the media files sorted in date order, most recent first.")
    private boolean m3u;

    /**
     * Utility method to cast unchecked Iterable types to checked ones. This can cause a class cast
     * exception, but in the cases where this is used, that is acceptable and unexpected.
     *
     * @param <T>
     *            item type.
     * @param <U>
     *            collection type.
     * @param obj
     *            the uncasted collection.
     * @param type
     *            the item type that is required.
     * @return The typed collection.
     */
    @SuppressWarnings("unchecked")
    private <T, U extends Iterable<T>> U iter(Iterable obj, Class<T> type) {
        return (U) obj;
    }

    /**
     * @param objects
     *            the objects to check.
     * @return true if any of the objects are not null, false otherwise.
     */
    private boolean anyNonNull(Object... objects) {
        for (Object object : objects) {
            if (object != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param <T>
     *            the type
     * @param vals
     *            the values to check.
     * @return the first non-null value or null if all values are null.
     */
    private <T> T firstNonNull(T... vals) {
        for (T t : vals) {
            if (t != null) {
                return t;
            }
        }

        return null;
    }

    /**
     * Deletes the oldest media file in the given directory iff the size of those items is >=
     * episodesToKeep.
     *
     * @param dir
     *            the directory to look in.
     */
    private void deleteOldest(File dir) {
        if (episodesToKeep > 0) {
            File[] files = sort(dir.listFiles(EXTENSION_FILTER), LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
            if (files.length >= episodesToKeep) {
                System.out.println("Deleting file " + files[0].getAbsolutePath());

                FileUtils.deleteQuietly(files[0]);
                deleteOldest(dir);
            }
        } else if (verbose) {
            System.out.println("Keeping all old media files.");
        }
    }

    private File[] sort(File[] files, Comparator<File> comparator) {
        Arrays.sort(files, comparator);
        return files;
    }

    private void checkFile() {
        File d = new File(directory);

        if (!d.exists()) {
            System.err.println("The directory " + d.getAbsolutePath() + " does not exist.");
            System.exit(1);
        }

        if (!d.isDirectory()) {
            System.err.println("The path " + d.getAbsolutePath() + " is not a directory.");
            System.exit(1);
        }
    }

    /**
     * Downloads a number of enclosure files to the download dir. Also can delete media files in the
     * download directory so as not to exceed max episodes.
     *
     * @param url
     *            the url to look at.
     * @throws IllegalArgumentException
     * @throws FeedException
     * @throws IOException
     */
    private void downloadMedia(String url) throws IllegalArgumentException, FeedException, IOException, ID3Exception {
        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        File dir = new File(directory);
        int count = 0;
        dir.mkdirs();

        if (verbose) {
            System.out.println("Using base directory " + dir.getAbsolutePath());
        }

        for (SyndEntry entry : iter(feed.getEntries(), SyndEntry.class)) {
            for (SyndEnclosure enc : iter(entry.getEnclosures(), SyndEnclosure.class)) {
                URL u = new URL(enc.getUrl());
                File media = null;

                if (keepFileName) {
                    media = new File(dir, FilenameUtils.getName(u.getPath()));
                } else {
                    String ext = FilenameUtils.getExtension(u.getPath());
                    String sane = entry.getTitle().replaceAll("[^a-zA-Z0-9\\._\\- ]+", "").trim();
                    media = new File(dir, sane + "." + ext.toLowerCase());
                }

                if (!media.exists()) {
                    // get rid of something since we are about to get something new:
                    deleteOldest(dir);

                    // media doesn't exist, download it:
                    System.out.println("Downloading to " + media.getAbsolutePath() + ", size: "
                                       + FileUtils.byteCountToDisplaySize(u.openConnection().getContentLength()));
                    FileUtils.copyURLToFile(u, media);

                    // add tags if available:
                    mergeTags(media, feed, entry);

                    // set time to entry type... this is needed to keep oldest/newest:
                    Date date = firstNonNull(entry.getUpdatedDate(), entry.getPublishedDate());
                    if (date != null) {
                        media.setLastModified(date.getTime());
                    } else if (verbose) {
                        System.out.println("No date was provided for file " + media.getAbsolutePath()
                                           + " it is likely that the timestamp for this file is wrong and could"
                                           + " lead to problems on subsequent runs.");
                    }
                } else if (verbose) {
                    System.out.println("Skipping file " + media.getAbsolutePath() + " since it already exists.");
                }

                count++;
                if (count >= episodesToKeep) {
                    return;
                }
            }
        }
    }

    /**
     * Generates an m3u file of the files in the base directory sorted by date, newest first.
     *
     * @throws IOException
     *             unable to write the file.
     */
    public void generateM3u() throws IOException {
        if (m3u) {
            File[] files =
                sort(new File(directory).listFiles(EXTENSION_FILTER), LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            List<String> fileNames = new ArrayList<String>(files.length);
            for (File f : files) {
                fileNames.add(f.getName());
            }

            File playlist = new File(directory, M3U_FILE_NAME);
            FileUtils.writeLines(playlist, fileNames);
            playlist.setLastModified(files[0].lastModified());
            System.out.println("Wrote playlist file " + playlist.getAbsolutePath());
        }
    }

    /**
     * @param categories
     *            the List of categories to check.
     * @return the first category name, or null if nothing is found.
     */
    @SuppressWarnings("unchecked")
    private String category(List categories) {
        if (categories != null && categories.size() > 0) {
            return ((SyndCategory) categories.get(0)).getName();
        }

        return null;
    }

    /**
     * Sets the tag values based on command options.
     *
     * @param media
     *            the media file.
     * @param feed
     *            the feed.
     * @param entry
     *            the entry.
     * @throws ID3Exception
     *             something bad happened in setting tag values or saving.
     */
    private void mergeTags(File media, SyndFeed feed, SyndEntry entry) throws ID3Exception {
        if ((readTagsFromFeed || anyNonNull(artist, album, comment, genre, title, year))
            && FilenameUtils.isExtension(media.getAbsolutePath().toLowerCase(), MP3_EXTENSION)) {

            // set tag values:
            MP3File tagged = new MP3File(media);
            ID3V2Tag tag = firstNonNull(tagged.getID3V2Tag(), new ID3V2_3_0Tag());

            if (readTagsFromFeed) {
                if (artUrl == null && feed.getImage() != null) {
                    artUrl = feed.getImage().getUrl();
                }

                tag.setArtist(firstNonNull(entry.getAuthor(), feed.getAuthor()));
                tag.setAlbum(feed.getTitle());
                tag.setTitle(firstNonNull(entry.getTitle(), feed.getTitle()));
                tag.setComment(firstNonNull(entry.getDescription().getValue(), feed.getDescription()));
                tag.setGenre(firstNonNull(category(entry.getCategories()), category(feed.getCategories())));

                Date d = firstNonNull(entry.getUpdatedDate(), entry.getPublishedDate(), new Date());
                if (d != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    tag.setYear(c.get(Calendar.YEAR));
                }
            }

            if (album != null) {
                tag.setAlbum(album);
            }

            if (artist != null) {
                tag.setArtist(artist);
            }

            if (comment != null) {
                tag.setComment(comment);
            }

            if (genre != null) {
                tag.setGenre(genre);
            }

            if (title != null) {
                tag.setTitle(title);
            }

            if (year != null) {
                tag.setYear(year);
            }

            loadImage(tag);

            tagged.setID3Tag(tag);
            tagged.sync();
        }
    }

    private void loadImage(ID3V2Tag tag) {
        if (tag instanceof ID3V2_3_0Tag && artUrl != null) {
            try {
                ((ID3V2_3_0Tag) tag).addAPICFrame(new APICID3V2Frame(null, PictureType.FrontCover, "",
                    IOUtils.toByteArray(new URL(artUrl))));
            } catch (Exception ex) {
                System.err.println("Unable to set cover art from url " + artUrl);

                if (debug) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void run() {
        validate(rssUrls != null, "A feed URL is required.");
        checkFile();

        for (String rssUrl : rssUrls) {
            try {
                downloadMedia(rssUrl);
            } catch (Exception e) {
                System.err.println("Error in downloading media from url " + rssUrl + ", " + e.getMessage());

                if (debug) {
                    e.printStackTrace();
                }
            }
        }

        try {
            generateM3u();
        } catch (IOException ioe) {
            System.err.println("Error in generating the m3u playlist file.");

            if (debug) {
                ioe.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new RssDownloadDriver().command("rss-downloader", args);
    }
}
