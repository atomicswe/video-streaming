const form = document.getElementById('uploadForm');
const loadingIndicator = document.getElementById('loadingIndicator');
const result = document.getElementById('result');

const fileInput = document.getElementById('videoFile');
const fileName = document.getElementById('fileName');
if (fileInput && fileName) {
    fileInput.addEventListener('change', function () {
        fileName.textContent = fileInput.files.length > 0 ? fileInput.files[0].name : 'No file selected.';
    });
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(form);

    try {
        const uploadResponse = await fetch('http://localhost:1221/videos/upload', {
            method: 'POST',
            body: formData,
            mode: 'cors',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!uploadResponse.ok) {
            throw new Error('Upload failed');
        }

        const uploadData = await uploadResponse.json();
        const jobId = uploadData.jobId;

        loadingIndicator.classList.remove('hidden');
        result.classList.add('hidden');

        const checkStatus = async () => {
            try {
                const statusResponse = await fetch(`http://localhost:1221/videos/upload/status/${jobId}`, {
                    method: 'GET',
                    mode: 'cors',
                    headers: {
                        'Accept': 'application/json'
                    }
                });

                if (!statusResponse.ok) {
                    throw new Error('Status check failed');
                }

                const statusData = await statusResponse.json();

                if (statusData.status === 'completed') {
                    loadingIndicator.classList.add('hidden');
                    result.classList.remove('hidden');
                    return;
                }

                setTimeout(checkStatus, 2000);
            } catch (error) {
                console.error('Error checking status:', error);
                loadingIndicator.classList.add('hidden');
            }
        };

        checkStatus();
    } catch (error) {
        console.error('Error uploading file:', error);
        loadingIndicator.classList.add('hidden');
    }
});
