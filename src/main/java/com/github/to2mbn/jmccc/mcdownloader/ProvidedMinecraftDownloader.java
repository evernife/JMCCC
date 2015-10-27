package com.github.to2mbn.jmccc.mcdownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import org.json.JSONObject;
import com.github.to2mbn.jmccc.mcdownloader.download.DownloadTask;
import com.github.to2mbn.jmccc.mcdownloader.download.FileDownloadTask;
import com.github.to2mbn.jmccc.mcdownloader.download.MemoryDownloadTask;
import com.github.to2mbn.jmccc.mcdownloader.download.ResultProcessor;
import com.github.to2mbn.jmccc.mcdownloader.provider.JarLibraryDownloadHandler;
import com.github.to2mbn.jmccc.mcdownloader.provider.LibraryDownloadHandler;
import com.github.to2mbn.jmccc.mcdownloader.provider.MinecraftDownloadProvider;
import com.github.to2mbn.jmccc.mcdownloader.provider.MojangDownloadProvider;
import com.github.to2mbn.jmccc.mcdownloader.provider.PackLibraryDownloadHandler;
import com.github.to2mbn.jmccc.mcdownloader.provider.XZPackLibraryDownloadHandler;
import com.github.to2mbn.jmccc.option.MinecraftDirectory;
import com.github.to2mbn.jmccc.version.Library;

public class ProvidedMinecraftDownloader implements MinecraftDownloader {

	public static ProvidedMinecraftDownloader buildDefault() {
		ProvidedMinecraftDownloader downloader = new ProvidedMinecraftDownloader(new MojangDownloadProvider());
		downloader.registerLibraryDownloadHandler(".jar", new JarLibraryDownloadHandler());
		downloader.registerLibraryDownloadHandler(".pack", new PackLibraryDownloadHandler());
		downloader.registerLibraryDownloadHandler(".pack.xz", new XZPackLibraryDownloadHandler());
		return downloader;
	}

	private MinecraftDownloadProvider provider;
	private Map<String, LibraryDownloadHandler> libraryHandlers = new ConcurrentSkipListMap<>(new Comparator<String>() {

		@Override
		public int compare(String o1, String o2) {
			int result = o2.length() - o1.length();
			if (result == 0) {
				result = o1.compareTo(o2);
			}
			return result;
		}
	});

	public ProvidedMinecraftDownloader(MinecraftDownloadProvider provider) {
		Objects.requireNonNull(provider);
		this.provider = provider;
	}

	@Override
	public DownloadTask<RemoteVersionList> versionList() {
		return new MemoryDownloadTask(provider.getVersionList()).andThen(new ResultProcessor<byte[], RemoteVersionList>() {

			@Override
			public RemoteVersionList process(byte[] arg) throws IOException {
				return RemoteVersionList.fromJson(new JSONObject(new String(arg, "UTF-8")));
			}
		});
	}

	@Override
	public DownloadTask<Set<Asset>> assetsIndex(final MinecraftDirectory mcdir, final String version) {
		return new MemoryDownloadTask(provider.getAssetIndex(version)).andThen(new ResultProcessor<byte[], Set<Asset>>() {

			@Override
			public Set<Asset> process(byte[] arg) throws IOException {
				File indexDir = new File(mcdir.getAssets(), "indexes");
				if (!indexDir.exists()) {
					indexDir.mkdirs();
				}
				try (FileOutputStream out = new FileOutputStream(new File(indexDir, version + ".json"))) {
					out.write(arg);
				}
				return Asset.fromJson(new JSONObject(new String(arg, "UTF-8")));
			}
		});
	}

	@Override
	public DownloadTask<Object> gameJar(MinecraftDirectory mcdir, String version) {
		return new FileDownloadTask(provider.getGameJar(version), mcdir.getVersionJar(version));
	}

	@Override
	public DownloadTask<Object> gameVersionJson(MinecraftDirectory mcdir, String version) {
		return new FileDownloadTask(provider.getGameVersionJson(version), mcdir.getVersionJson(version));
	}

	@Override
	public DownloadTask<Object> library(MinecraftDirectory mcdir, Library library) {
		URI uri = provider.getLibrary(library);
		String path = uri.getPath();
		LibraryDownloadHandler handler = null;
		for (Entry<String, LibraryDownloadHandler> entry : libraryHandlers.entrySet()) {
			if (path.endsWith(entry.getKey())) {
				handler = entry.getValue();
				break;
			}
		}
		if (handler == null) {
			throw new IllegalArgumentException("unable to resolve library download handler, path: " + path);
		}
		return handler.createDownloadTask(new File(mcdir.getLibraries(), library.getPath()), library, uri);
	}

	@Override
	public DownloadTask<Object> asset(MinecraftDirectory mcdir, Asset asset) {
		return new FileDownloadTask(provider.getAsset(asset), new File(mcdir.getAssets(), "objects/" + asset.getPath()));
	}

	public MinecraftDownloadProvider getProvider() {
		return provider;
	}

	public void registerLibraryDownloadHandler(String postfix, LibraryDownloadHandler handler) {
		Objects.requireNonNull(postfix);
		Objects.requireNonNull(handler);
		libraryHandlers.put(postfix, handler);
	}

	public void unregisterLibraryDownloadHandler(String postfix, LibraryDownloadHandler handler) {
		libraryHandlers.remove(postfix);
	}

}
