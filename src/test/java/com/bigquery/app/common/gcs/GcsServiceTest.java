package com.bigquery.app.common.gcs;

import com.bigquery.app.common.exception.PermissionException;
import com.bigquery.app.common.exception.ResourceNotFoundException;
import com.bigquery.app.common.exception.ValidationException;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.bigquery.app.util.GcsMockUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class GcsServiceTest {

    private static final String REQUIRED_PERMISSIONS = "perm1,perm2";

    @Mock
    private Storage mockStorage;

    private GcsService gcsService;

    @BeforeEach
    public void setUp() {
        gcsService = new GcsService(mockStorage, REQUIRED_PERMISSIONS);
    }

    @Test
    public void testGetBucket_success() {
        // given
        String gcsUri = "gs://my-bucket";
        Bucket bucketMock = createMockBucket("my-bucket");
        when(mockStorage.get("my-bucket")).thenReturn(bucketMock);

        // when
        Bucket returnedBucket = gcsService.getBucket(gcsUri);

        // then
        assertNotNull(returnedBucket);
        verify(mockStorage).get("my-bucket");
    }

    @Test
    public void testGetBucket_notFound() {
        // given
        String gcsUri = "gs://my-bucket";
        when(mockStorage.get("my-bucket")).thenReturn(null);

        // when + then
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> gcsService.getBucket(gcsUri));
        assertTrue(exception.getMessage().contains("GCS bucket"));
    }

    @Test
    public void testValidateGcsBucket_success() {
        // given
        mockBucketPermissions(mockStorage, "my-bucket", List.of("perm1", "perm2"));

        Bucket bucketMock = createMockBucket("my-bucket");
        when(mockStorage.get("my-bucket")).thenReturn(bucketMock);

        // when + then
        assertDoesNotThrow(() -> gcsService.validateGcsBucket("gs://my-bucket"));
    }

    @Test
    public void testValidateGcsBucket_insufficientPermissions() {
        // given
        Bucket bucketMock = createMockBucket("my-bucket");
        when(mockStorage.get("my-bucket")).thenReturn(bucketMock);

        List<Boolean> permissionResults = List.of(true, false);
        when(mockStorage.testIamPermissions(eq("my-bucket"), anyList())).thenReturn(permissionResults);

        // when + then
        Exception exception = assertThrows(PermissionException.class, () -> gcsService.validateGcsBucket("gs://my-bucket"));
        assertTrue(exception.getMessage().contains("Permission denied: [perm1, perm2]"));
    }

    @Test
    public void testValidateFileFormat_success() {
        // given + when + then
        assertDoesNotThrow(() -> gcsService.validateFileFormat("folder/table1.AVRO", "AVRO"));
    }

    @Test
    public void testValidateFileFormat_failure() {
        // given + when + then
        Exception exception = assertThrows(ValidationException.class, () ->
                gcsService.validateFileFormat("folder/table1.txt", "AVRO"));
        assertTrue(exception.getMessage().contains("Extension mismatch"));
    }

    @Test
    public void testFindTableNamesFromGcsFiles_success() {
        // given
        String basePath = "gs://my-bucket/folder";
        Pattern pattern = Pattern.compile("^(.*)\\.AVRO$");
        String expectedFormat = "AVRO";

        Blob blobMock = createMockBlob("folder/table1.AVRO");

        Page<Blob> pageMock = (Page<Blob>) mock(Page.class);
        when(pageMock.iterateAll()).thenReturn(List.of(blobMock));

        Bucket bucketMock = mock(Bucket.class);
        when(bucketMock.list(ArgumentMatchers.<Storage.BlobListOption>any()))
                .thenReturn(pageMock);

        // when
        Set<String> tableNames = gcsService.findTableNamesFromGcsFiles(bucketMock, basePath, pattern, expectedFormat);

        // then
        assertEquals(1, tableNames.size());
        assertTrue(tableNames.contains("table1"));
    }
}