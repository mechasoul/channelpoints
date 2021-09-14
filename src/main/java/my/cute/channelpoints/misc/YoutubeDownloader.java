package my.cute.channelpoints.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * i don't particularly want to upkeep my own adaptation of vlc youtube parser and
 * i also don't have the knowledge to do so on my own so i'm just going to use 
 * youtube-dl (https://github.com/ytdl-org/youtube-dl). seems really well maintained
 * and has a lot of functionality. credit 2 them
 * <p>
 * <b>this class requires youtube-dl.exe in working directory or nothing works</b>
 */
public final class YoutubeDownloader {

	private final ExecutorService executor;
	
	public YoutubeDownloader() {
		this(ForkJoinPool.commonPool());
	}
	
	public YoutubeDownloader(ExecutorService executor) {
		this.executor = executor;
	}
	
	/**
	 * uses youtube-dl.exe to parse the given youtube link into a direct video link. returns
	 * a future that will complete with the direct video link upon successful completion of
	 * the youtube-dl process. note the returned video is limited to 720p or lower
	 * <p>
	 * this method and the youtube-dl process run in the executor
	 * for this instance of YoutubeDownloader
	 * @param link the youtube link (i'm not sure what happens if the given link isn't a 
	 * youtube link?)
	 * @return a completablefuture that will be completed with a direct video link for the
	 * given youtube link once the youtube-dl process finishes. if youtube-dl exits
	 * abnormally, the future will be completed with null. if an IOException or 
	 * InterruptedException occur during the process, the future will complete exceptionally
	 * with a CompletionException wrapped around the given exception (note IOException can
	 * occur if an IOException occurs while youtube-dl is writing the obtained link to temp
	 * file, and an InterruptedException can occur if one happens while waiting for the 
	 * youtube-dl process to finish)
	 */
	public CompletableFuture<String> getDirectLink(String link) {
		return CompletableFuture.supplyAsync(() -> {
				try {
					Path outputPath = Files.createTempFile(null, ".tmp");
					Process dl = new ProcessBuilder("youtube-dl.exe", "--no-playlist", "-g", "-f \"best[height<=?720]\"", link)
							.redirectOutput(outputPath.toFile())
							.start();
					if(!dl.waitFor(20, TimeUnit.SECONDS)) {
						//process timed out
						dl.destroy();
						throw new CompletionException(new TimeoutException("youtube-dl.exe timed out with link: " + link));
					}
					String returnValue = null;
					try (BufferedReader reader = Files.newBufferedReader(outputPath, StandardCharsets.UTF_8)) {
						String firstLine = reader.readLine();
						String secondLine = reader.readLine();
						if(secondLine != null) {
							returnValue = secondLine;
						} else if (firstLine != null) {
							returnValue = firstLine;
						} 
					} 
					
					Files.delete(outputPath);
					
					return returnValue;
				} catch (IOException | InterruptedException e) {
					throw new CompletionException(e);
				}
		}, this.executor);
	}
}
