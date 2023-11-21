package org.wallentines.mcore.data;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileCodecRegistry;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

/**
 * A class for loading and saving data files from a directory on disk.
 */
public class DataManager {

    private final File searchDirectory;
    private final FileCodecRegistry fileCodecRegistry;
    private final HashMap<String, FileWrapper<ConfigObject>> openFiles = new HashMap<>();
    private final Queue<String> opened = new ArrayDeque<>();

    /**
     * The maximum number of files that will be cached at any given time
     */
    public int cacheSize = 8;


    /**
     * Constructs a new data manager with the given search directory, using the default codec registry
     * @param searchDirectory The directory to search for and save data files in
     */
    public DataManager(File searchDirectory) {
        this(searchDirectory, MidnightCoreAPI.FILE_CODEC_REGISTRY);
    }

    /**
     * Constructs a new data manager with the given search directory and codec registry
     * @param searchDirectory The directory to search for and save data files in
     * @param fileCodecRegistry The codecs to use to decode and encode data files
     */
    public DataManager(File searchDirectory, FileCodecRegistry fileCodecRegistry) {
        this.searchDirectory = searchDirectory;
        this.fileCodecRegistry = fileCodecRegistry;
    }

    /**
     * Gets the data associated with a specific key
     * @param key The key to lookup
     * @return The data associated with the given key
     */
    public ConfigSection getData(String key) {

        return getWrapper(key).getRoot().asSection();
    }


    /**
     * Gets the data associated with a specific key
     * @param key The key to lookup
     * @return The data associated with the given key
     */
    @Nullable
    public ConfigSection getDataOrNull(String key) {

        FileWrapper<ConfigObject> wrapper = getWrapper(key, false);
        if(wrapper == null) return null;

        return wrapper.getRoot().asSection();
    }


    /**
     * Saves the given data to a file associated with the given key
     * @param key The key to save
     * @param section The data to save
     */
    public void save(String key, ConfigSection section) {

        FileWrapper<ConfigObject> obj = getWrapper(key);
        obj.setRoot(section);
        obj.save();
    }

    /**
     * Clears all data associated with the given key and deletes the file
     * @param key The key to clear
     * @return Whether clearing was successful
     */
    public boolean clear(String key) {

        FileWrapper<ConfigObject> obj = getWrapper(key);
        openFiles.remove(key);

        if(!obj.getFile().delete()) {
            MidnightCoreAPI.LOGGER.error("Unable to delete data file " + obj.getFile().getAbsolutePath() + "!");
            return false;
        }
        return true;
    }

    /**
     * Clears all references to files in the cache.
     */
    public void clearCache() {

        opened.clear();
        openFiles.clear();
    }


    private FileWrapper<ConfigObject> getWrapper(String key) {
        return getWrapper(key, true);
    }

    private FileWrapper<ConfigObject> getWrapper(String key, boolean create) {

        if(opened.size() == cacheSize) {
            openFiles.remove(opened.remove());
        }

        return openFiles.computeIfAbsent(key, k -> {

            FileWrapper<ConfigObject> wrapper;

            if(create) {
                wrapper = fileCodecRegistry.findOrCreate(ConfigContext.INSTANCE, k, searchDirectory);
            } else {
                wrapper = fileCodecRegistry.find(ConfigContext.INSTANCE, k, searchDirectory);
            }

            if(wrapper != null) {
                wrapper.load();
                if (wrapper.getRoot() == null || !wrapper.getRoot().isSection()) {
                    wrapper.setRoot(new ConfigSection());
                }
                opened.add(key);
            }

            return wrapper;
        });
    }


}
