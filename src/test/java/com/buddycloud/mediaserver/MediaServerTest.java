package com.buddycloud.mediaserver;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Properties;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;

import com.buddycloud.mediaserver.business.jdbc.MetaDataSource;
import com.buddycloud.mediaserver.business.model.Media;
import com.buddycloud.mediaserver.business.util.AudioUtils;
import com.buddycloud.mediaserver.business.util.ImageUtils;
import com.buddycloud.mediaserver.business.util.VideoUtils;
import com.buddycloud.mediaserver.commons.MediaServerConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class MediaServerTest {
	
	protected static final String TEST_MEDIA_STORAGE_ROOT = "/tmp";
	protected static final String TESTIMAGE_NAME = "testimage.jpg";
	protected static final String TESTVIDEO_NAME = "testvideo.avi";
	protected static final String TESTAVATAR_NAME = "testavatar.jpg";
	protected static final String TESTFILE_PATH = "resources/tests/";
	
	protected static final String TEST_OUTPUT_DIR = "test";
	protected static final String MEDIA_ID = generateRandomString();

	protected static final String BASE_TOKEN = "secret";
	protected static final String BASE_CHANNEL = "mediaservertest@buddycloud.org";
	protected static final String BASE_USER = "mediaservertest@buddycloud.org";
	protected static final String BASE_URL = "http://localhost:8080";
	

	protected RestletTest restletTest;
	protected XMPPTest xmppTest;
	protected Properties configuration;
	protected MetaDataSource dataSource;
	protected Gson gson;
	
	
	@Before
	public void setUp() throws Exception {
		configuration = MediaServerConfiguration.getInstance().getConfiguration();
		configuration.setProperty(MediaServerConfiguration.MEDIA_STORAGE_ROOT_PROPERTY, TEST_MEDIA_STORAGE_ROOT);
		
		dataSource = new MetaDataSource();
		gson = new GsonBuilder().setDateFormat(DateFormat.FULL, DateFormat.FULL).create();
		
		start();
	    testSetUp();
	    
	    Thread.sleep(1000);
	}
	
	@After
	public void tearDown() throws Exception {
		restletTest.shutdown();
		xmppTest.shutdown();
		testTearDown();
	}
	
	protected void start() throws Exception {
		restletTest = new RestletTest();
		restletTest.start(configuration);
		
		xmppTest = new XMPPTest();
		xmppTest.start(configuration);
	}
	
	protected Media buildMedia(String mediaId, String filePath) throws Exception {
		File file = new File(filePath);
		String fileName = file.getName();
		String extension = fileName.substring(fileName.indexOf(".") + 1);
		
		Media media = new Media();
		media.setId(mediaId);
		media.setFileName(fileName);
		media.setEntityId(BASE_CHANNEL);
		media.setAuthor(BASE_USER);
		media.setDescription("A description");
		media.setTitle("A title");
		media.setFileSize(file.length());
		media.setShaChecksum(getFileShaChecksum(file));
		media.setFileExtension(extension);
		media.setMimeType(new MimetypesFileTypeMap().getContentType(file));
		
		if (ImageUtils.isImage(extension)) {
			BufferedImage img = ImageIO.read(file);
			media.setHeight(img.getHeight());
			media.setWidth(img.getWidth());
		} else if (VideoUtils.isVideo(extension)) {
			VideoUtils videoUtils = new VideoUtils(file);
			media.setLength(videoUtils.getVideoLength());
			media.setHeight(videoUtils.getVideoHeight());
			media.setWidth(videoUtils.getVideoWidth());
		} else if (AudioUtils.isAudio(extension)) {
			media.setLength(AudioUtils.getAudioLength(file));
		}
		
		return media;
	}
	
	private String getFileShaChecksum(File file) throws IOException {
		return DigestUtils.shaHex(FileUtils.openInputStream(file));
	}
	
	protected static String generateRandomString() {
		return RandomStringUtils.random(20, true, true);
	}
	
	protected abstract void testSetUp() throws Exception;
	
	protected abstract void testTearDown() throws Exception;
}
