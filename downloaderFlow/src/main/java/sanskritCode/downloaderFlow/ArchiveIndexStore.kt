package sanskritCode.downloaderFlow

import android.util.Log

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.TextHttpResponseHandler

import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

enum class ArchiveStatus {
    NOT_TRIED, DOWNLOAD_SUCCESS, DOWNLOAD_FAILURE, EXTRACTION_SUCCESS, EXTRACTION_FAILURE
}

class ArchiveInfo(var url: String) : Serializable {
    var status = ArchiveStatus.NOT_TRIED
    var downloadedArchiveBasename: String? = null

    override fun toString(): String {
        return ("\n status: " + status
                + "\n downloadedArchiveBasename: " + downloadedArchiveBasename
                + "\n url: " + url)
    }
}

class ArchiveIndexStore(val indexIndexorum: String) : Serializable {
    private val LOGGER_TAG = javaClass.getSimpleName()
    // ArchiveIndexStore must be serializable, hence we use specific class names below.
    private val indexUrls = LinkedHashMap<String, String>()
    val indexesSelected: BiMap<String, String> = HashBiMap.create(100)
    var indexedArchives: MutableMap<String, List<String>> = LinkedHashMap()
    var archivesSelectedMap: MutableMap<String, ArchiveInfo> = HashMap()
    var autoUnselectedArchives = 0

    override fun toString(): String {
        return ("\nindexIndexorum: " + indexIndexorum
                + "\nindexUrls: " + indexUrls
                + "\nindexesSelected: " + indexesSelected
                + "\nindexedArchives: " + indexedArchives
                + "\nautoUnselectedArchives: " + autoUnselectedArchives
                + "\narchivesSelectedMap: " + archivesSelectedMap)
    }


    fun estimateArchivesSelectedMBs(): Int {
        var size = 0
        for (url in archivesSelectedMap.keys) {
            size += ArchiveNameHelper.getSize(url)
        }
        return size
    }

    fun getIndicesAddCheckboxes(activity: MainActivity) {
        if (indexUrls.isEmpty()) {
            val asyncHttpClient = AsyncHttpClient()
            asyncHttpClient.setEnableRedirects(true, true, true)
            asyncHttpClient.get(indexIndexorum, object : TextHttpResponseHandler() {
                override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String, throwable: Throwable) {
                    Log.e(LOGGER_TAG, ":getIndicesAddCheckboxes:" + "getIndices", throwable)
                }

                override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String) {
                    for (line in responseString.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                        val url = line.replace("<", "").replace(">", "")
                        val name = url.replace("https://raw.githubusercontent.com/|/tars/tars.MD|master/".toRegex(), "")
                        indexUrls[name] = url

                        indexesSelected[name] = url
                        Log.d(LOGGER_TAG, ":getIndicesAddCheckboxes:" + activity.getString(R.string.df_added_index_url) + url)
                    }
                    activity.addCheckboxes(indexUrls, indexesSelected)
                }
            })
        } else {
            activity.addCheckboxes(indexUrls, indexesSelected)
        }
    }

    // Populates indexedArchives
    fun getIndexedArchivesSetCheckboxes(getUrlActivity: GetUrlActivity) {
        if (indexedArchives.isEmpty()) {
            val asyncHttpClient = AsyncHttpClient()
            indexedArchives = LinkedHashMap()
            Log.i(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:" + "Will get archives from " + indexesSelected.size)
            asyncHttpClient.setEnableRedirects(true, true, true)
            for (name in indexesSelected.keys) {
                val url = indexesSelected[name]
                val archiveIndexStore = this

                try {
                    asyncHttpClient.get(url, object : TextHttpResponseHandler() {
                        override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String, throwable: Throwable) {
                            Log.e(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:" + "Failed ", throwable)
                            BaseActivity.largeLog(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:" + archiveIndexStore.toString())
                            val message = String.format(getUrlActivity.getString(R.string.df_index_download_failed), url)
                            getUrlActivity.topText?.setText(message)
                            // The below would result in
                            // java.lang.RuntimeException: android.os.FileUriExposedException: file:///storage/emulated/0/logcat.txt exposed beyond app through ClipData.Item.getUri()
                            // A known issue: https://github.com/loopj/android-async-http/issues/891
                            // getUrlActivity.sendLoagcatMail();

                            // Just proceed with the next dict.
                            indexedArchives[name] = listOf<String>(url!!.replace("[_/.]+".toRegex(), "_") + "_indexGettingFailed")
                            if (indexesSelected.size == indexedArchives.size) {
                                getUrlActivity.addCheckboxes(indexedArchives, null)
                            }
                        }

                        override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String) {
                            val urls = ArrayList<String>()
                            for (line in responseString.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                                val url1 = line.replace("<", "").replace(">", "")
                                urls.add(url1)
                                Log.d(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes: Added archive: " + url1)
                            }
                            Log.d(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:Index handled: $url")
                            indexedArchives[indexesSelected.inverse()[url]!!] = urls

                            if (indexesSelected.size == indexedArchives.size) {
                                getUrlActivity.addCheckboxes(indexedArchives, null)
                            }
                        }
                    })
                } catch (throwable: Throwable) {
                    Log.e(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:error with $url", throwable)
                    val message = String.format(getUrlActivity.getString(R.string.df_index_download_failed), url)
                    getUrlActivity.topText?.setText(message)
                    getUrlActivity.sendLoagcatMail()
                }

            }
        } else {
            getUrlActivity.addCheckboxes(indexedArchives, archivesSelectedMap.keys)
        }
    }
}
