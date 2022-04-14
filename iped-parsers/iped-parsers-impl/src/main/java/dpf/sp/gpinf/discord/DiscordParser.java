package dpf.sp.gpinf.discord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import dpf.sp.gpinf.discord.cache.CacheEntry;
import dpf.sp.gpinf.discord.cache.Index;
import dpf.sp.gpinf.discord.json.DiscordAttachment;
import dpf.sp.gpinf.discord.json.DiscordRoot;
import dpf.sp.gpinf.indexer.parsers.IndexerDefaultParser;
import dpf.sp.gpinf.indexer.util.EmptyInputStream;

import iped3.io.IItemBase;
import iped3.search.IItemSearcher;
import iped3.util.BasicProps;
import iped3.util.ExtraProperties;

/***
 * 
 * @author PCF Campanini
 *
 */
public class DiscordParser extends AbstractParser {

	private static Logger LOGGER = LoggerFactory.getLogger(DiscordParser.class);

	private static final long serialVersionUID = 1L;
	public static final String INDEX_MIME_TYPE = "application/x-discord-index";
	public static final String CHAT_MIME_TYPE = "application/x-discord-chat";
	public static final String MSG_MIME_TYPE = "message/x-discord-message";

	public static final String DATA_MIME_TYPE_V2_0 = "data-v20/x-discord-chat";
	public static final String DATA_MIME_TYPE_V2_1 = "data-v21/x-discord-chat";

	private static final Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType.parse(INDEX_MIME_TYPE));

	@Override
	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}

	@Override
	public void parse(InputStream indexFile, ContentHandler handler, Metadata metadata, ParseContext context)
			throws IOException, SAXException, TikaException {

		EmbeddedDocumentExtractor extractor = context.get(EmbeddedDocumentExtractor.class,
				new ParsingEmbeddedDocumentExtractor(context));

		IItemSearcher searcher = context.get(IItemSearcher.class);
		IItemBase item = context.get(IItemBase.class);

		if (searcher != null && item != null) {

			String parentPath = Paths.get(item.getPath()).getParent().toString().replace("\\", "\\\\");

			String commonQuery = BasicProps.EVIDENCE_UUID + ":" + item.getDataSource().getUUID() + " AND "
					+ BasicProps.PATH + ":\"" + parentPath + "\" AND " + BasicProps.CARVED + ":false AND NOT "
					+ BasicProps.TYPE + ":slack AND NOT " + BasicProps.LENGTH + ":0 AND NOT " + BasicProps.ISDIR
					+ ":true";

			List<IItemBase> externalFiles = searcher.search(commonQuery + " AND " + BasicProps.NAME + ":f");
			List<IItemBase> dataFiles = searcher.search(commonQuery + " AND " + BasicProps.NAME
					+ ":(\"data_0\"  OR \"data_1\" OR \"data_2\" OR \"data_3\")");

			Index index = new Index(indexFile, dataFiles, externalFiles);

			// Used to identify JSON files containing Discord chats
			CharSequence seq = "messages?limit=50";
			TikaException exception = null;
			
			for (CacheEntry ce : index.getLst()) {
				if (ce.getKey() != null && ce.getKey().contains(seq)) {
                    try (InputStream is = ce.getResponseDataStream()) {
                    	
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        List<DiscordRoot> discordRoot = mapper.readValue(is, new TypeReference<List<DiscordRoot>>() {
                        });

                        if (discordRoot.isEmpty())
                            continue;

                        try {
	                        //Checking if the image file is cached, to do so, iterates through all authors and attachments to check if they are in the case, comparing their attributes
	                        for (DiscordRoot dr : discordRoot) {
	                        	for (CacheEntry ce2 : index.getLst()) {
	                        		if (ce2.getKey() != null) {
	                        			//Checking avatar image
		                        		if (dr.getAuthor().avatar != null  && ce2.getKey().contains(dr.getAuthor().avatar) && !ce2.getName().contains("data")) {
		                        			dr.getAuthor().avatarURL = Base64.getEncoder().encodeToString(IOUtils.toByteArray(ce2.getResponseDataStream()));
		                        			break;
		                        		}
		                        		//Checking attachments image
		                        		for (DiscordAttachment att : dr.getAttachments()) {
			                        		if (ce2.getKey().contains(att.getFilename()) && !ce2.getName().contains("data")) {
			                        			for (IItemBase ib : externalFiles) {
			                        				if (ib.getName() != null && ib.getName().equals(ce2.getName())) {
			                        					att.setContent_type(getAttExtension(att.getUrl()));
			                        					att.setUrl(Base64.getEncoder().encodeToString(IOUtils.toByteArray(ib.getBufferedStream())));
			                        					break;
			                        				}
			                        			}
			                        		}
			                            }
	                        		}
	                        	}
	                        }
                        } catch (Exception ex){
                        	new TikaException(ex.getMessage());
                        }
                        
                        String chatName = "Discord Chat id=" + discordRoot.get(0).getId() + " author="
                                + discordRoot.get(0).getAuthor();

                        Metadata chatMeta = new Metadata();
						chatMeta.set("URL", ce.getRequestURL());
						chatMeta.set(TikaCoreProperties.TITLE, chatName);
                        chatMeta.set(IndexerDefaultParser.INDEXER_CONTENT_TYPE, CHAT_MIME_TYPE);

                        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, chatMeta);
                        byte[] relatorio = new DiscordHTMLReport().convertToHTML(discordRoot, xhtml);
                        
                        InputStream targetStream = new ByteArrayInputStream(relatorio);
                        extractor.parseEmbedded(targetStream, handler, chatMeta, true);
                        
                        extractMessages(chatName,discordRoot,handler,extractor);

                    } catch (Exception ex) {
                    	ex.printStackTrace();
                        if (exception == null) {
                            exception = new TikaException("DiscordParser parsing error.");
                        }
                        exception.addSuppressed(ex);
                    }
                }
            }

			if (exception != null) {
				throw exception;
			}
		}

	}

	private void extractMessages(String chatName, List<DiscordRoot> discordRoot, ContentHandler handler,
			EmbeddedDocumentExtractor extractor) throws SAXException, IOException {
		int msgCount = 0;
		for (DiscordRoot d : discordRoot) {
			Metadata meta = new Metadata();
			meta.set(TikaCoreProperties.TITLE, chatName + "_message_" + msgCount++);
			meta.set(IndexerDefaultParser.INDEXER_CONTENT_TYPE, MSG_MIME_TYPE);
			meta.set(ExtraProperties.MESSAGE_DATE, d.getTimestamp());
			meta.set(ExtraProperties.MESSAGE_BODY, d.getMessageContent());
			meta.set(ExtraProperties.USER_NAME, d.getAuthor().getFullUsername());

			if (d.getCall() != null) {
				meta.set(IndexerDefaultParser.INDEXER_CONTENT_TYPE, d.getCall().toString());
				meta.set("ended_timestamp", d.getCall().getEndedTimestamp().toString()); //$NON-NLS-1$
			}

			meta.set(BasicProps.LENGTH, ""); //$NON-NLS-1$
			extractor.parseEmbedded(new EmptyInputStream(), handler, meta, false);

		}
	}
	
	private String getAttExtension(String ext) {
		return ext.contains(".png") ? "image/png" :  (ext.contains(".webp") ? "image/webp" : (ext.contains(".jpg") ? "image/jpg" : (ext.contains(".mp4") ? "video/mp4" : (ext.contains(".webm") ? "video/webm" : ""))));
	}
}