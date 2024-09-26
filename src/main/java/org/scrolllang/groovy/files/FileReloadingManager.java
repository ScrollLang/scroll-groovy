package org.scrolllang.groovy.files;

import java.util.ArrayList;
import java.util.List;

// TODO change this class calls all the ReloadingBindings methods. Make a universal method for reloading groovy scripts and error handling.
public class FileReloadingManager {

	private static final List<FileReloading> fileReloadingList = new ArrayList<>();

	static void add(FileReloading fileReloading) {
		fileReloadingList.add(fileReloading);
	}

	public void reloadAllFiles() {
		for (FileReloading fileReloading : fileReloadingList) {
			fileReloading.reload();
		}
	}

}
