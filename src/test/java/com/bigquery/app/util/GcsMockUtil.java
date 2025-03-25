package com.bigquery.app.util;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static java.util.Objects.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GcsMockUtil {

    private static final List<String> STANDARD_PERMISSIONS = Arrays.asList(
            "storage.buckets.get",
            "storage.objects.list",
            "storage.objects.create"
    );

    public static Bucket createMockBucket(String bucketName) {
        Bucket mockBucket = mock(Bucket.class);
        when(mockBucket.getName()).thenReturn(bucketName);
        return mockBucket;
    }

    public static void mockBucketPermissions(Storage storage, String bucketName, List<String> permissions) {
        List<Boolean> grantedPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.size(); i++) {
            grantedPermissions.add(true);
        }
        when(storage.testIamPermissions(eq(bucketName), eq(permissions))).thenReturn(grantedPermissions);
    }

    public static void mockStandardPermissions(Storage storage, String bucketName) {
        mockBucketPermissions(storage, bucketName, STANDARD_PERMISSIONS);
    }

    public static List<Blob> mockBlobListing(Bucket bucket, String... blobPaths) {
        List<Blob> mockBlobs = new ArrayList<>();
        for (String path : blobPaths) {
            mockBlobs.add(createMockBlob(path));
        }

        Page<Blob> mockPage = mock(Page.class);
        when(mockPage.iterateAll()).thenReturn(mockBlobs);
        when(bucket.list(any(Storage.BlobListOption.class))).thenReturn(mockPage);

        return mockBlobs;
    }

    public static void mockBlobListing(Bucket bucket, Map<String, List<String>> datasetBlobs) {
        when(bucket.list(any(Storage.BlobListOption.class))).thenAnswer(invocation -> {
            Storage.BlobListOption option = invocation.getArgument(0);
            String prefix = extractPrefixFromOption(option);

            if (isNull(prefix)) {
                Page<Blob> emptyPage = mock(Page.class);
                when(emptyPage.iterateAll()).thenReturn(Collections.emptyList());
                return emptyPage;
            }

            for (String datasetPath : datasetBlobs.keySet()) {
                if (prefix.contains(datasetPath)) {
                    List<String> blobPaths = datasetBlobs.get(datasetPath);
                    List<Blob> mockBlobs = new ArrayList<>();

                    for (String path : blobPaths) {
                        mockBlobs.add(createMockBlob(path));
                    }

                    Page<Blob> mockPage = mock(Page.class);
                    when(mockPage.iterateAll()).thenReturn(mockBlobs);
                    return mockPage;
                }
            }

            Page<Blob> emptyPage = mock(Page.class);
            when(emptyPage.iterateAll()).thenReturn(Collections.emptyList());
            return emptyPage;
        });
    }

    public static Blob createMockBlob(String path) {
        Blob mockBlob = mock(Blob.class);
        when(mockBlob.getName()).thenReturn(path);
        return mockBlob;
    }

    public static Blob createMockBlob(String path, String contentType) {
        Blob mockBlob = createMockBlob(path);
        when(mockBlob.getContentType()).thenReturn(contentType);
        return mockBlob;
    }

    public static ArgumentCaptor<String> createBucketNameCaptor() {
        return ArgumentCaptor.forClass(String.class);
    }

    public static ArgumentCaptor<List<String>> createPermissionsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private static String extractPrefixFromOption(Storage.BlobListOption option) {
        if (isNull(option)) {
            return null;
        }

        String optionString = option.toString();

        if (optionString.contains("val=")) {
            int startIndex = optionString.indexOf("val=") + 4;
            int endIndex = optionString.indexOf('}', startIndex);

            if (endIndex == -1) {
                endIndex = optionString.length();
            }

            return optionString.substring(startIndex, endIndex);
        }
        return null;
    }

    public static Bucket setupMockGcsEnvironment(Storage storage, String bucketName) {
        Bucket mockBucket = createMockBucket(bucketName);
        when(storage.get(bucketName)).thenReturn(mockBucket);
        mockStandardPermissions(storage, bucketName);
        return mockBucket;
    }
}