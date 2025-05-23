document.addEventListener('DOMContentLoaded', function () {
    const videoList = document.getElementById('videoList');
    const prevPage = document.getElementById('prevPage');
    const nextPage = document.getElementById('nextPage');
    const pageInfo = document.getElementById('pageInfo');
    const feedbackMessage = document.getElementById('feedbackMessage');
    const feedbackText = document.getElementById('feedbackText');

    let currentPage = 0;
    const pageSize = 10;
    let totalVideos = 0;

    function showFeedback(message, isError = false) {
        feedbackMessage.classList.remove('hidden');
        feedbackMessage.classList.remove('bg-green-100', 'bg-red-100');
        feedbackMessage.classList.add(isError ? 'bg-red-100' : 'bg-green-100');
        feedbackText.textContent = message;
        
        setTimeout(() => {
            feedbackMessage.classList.add('hidden');
        }, 3000);
    }

    async function fetchVideos(page = 0) {
        try {
            const response = await fetch('http://localhost:1221/videos', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ page, pageSize })
            });
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error fetching videos:', error);
            showFeedback('Error loading videos', true);
            return [];
        }
    }

    function formatVideoName(filename) {
        let namePart = filename;
        if (filename.includes('-')) {
            namePart = filename.split('-').slice(1).join('-');
        }
        if (namePart.endsWith('.mp4')) {
            namePart = namePart.slice(0, -4);
        }
        namePart = namePart.replace(/_/g, ' ');
        namePart = namePart.charAt(0).toUpperCase() + namePart.slice(1);
        return namePart;
    }

    async function deleteVideo(filename) {
        try {
            const response = await fetch(`http://localhost:1221/videos/${filename}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                showFeedback('Video deleted successfully');
                const videos = await fetchVideos(currentPage);
                updateVideoList(videos);
                updatePagination(totalVideos);
            } else {
                showFeedback('Error deleting video', true);
            }
        } catch (error) {
            console.error('Error deleting video:', error);
            showFeedback('Error deleting video', true);
        }
    }

    function updateVideoList(videos) {
        videoList.innerHTML = '';
        videos.forEach(video => {
            const videoItem = document.createElement('div');
            videoItem.className = 'flex items-center justify-between p-4 bg-gray-50 rounded-lg';
            videoItem.innerHTML = `
                <div>
                    <h3 class="font-medium text-gray-800">${formatVideoName(video.name)}</h3>
                    <p class="text-sm text-gray-600">${new Date(video.created).toLocaleDateString()}</p>
                </div>
                <button class="delete-btn px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition-colors cursor-pointer">
                    Delete
                </button>
            `;

            const deleteBtn = videoItem.querySelector('.delete-btn');
            deleteBtn.addEventListener('click', () => {
                if (confirm('Are you sure you want to delete this video?')) {
                    deleteVideo(video.name);
                }
            });

            videoList.appendChild(videoItem);
        });
    }

    function updatePagination(total) {
        const totalPages = Math.ceil(total / pageSize);
        pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages}`;
        prevPage.disabled = currentPage === 0;
        nextPage.disabled = currentPage === totalPages - 1;
    }

    prevPage.addEventListener('click', async () => {
        if (currentPage > 0) {
            currentPage--;
            const videos = await fetchVideos(currentPage);
            updateVideoList(videos);
            updatePagination(totalVideos);
        }
    });

    nextPage.addEventListener('click', async () => {
        const totalPages = Math.ceil(totalVideos / pageSize);
        if (currentPage < totalPages - 1) {
            currentPage++;
            const videos = await fetchVideos(currentPage);
            updateVideoList(videos);
            updatePagination(totalVideos);
        }
    });

    async function initialize() {
        const videos = await fetchVideos();
        totalVideos = videos.length;
        updateVideoList(videos);
        updatePagination(totalVideos);
    }

    initialize();
});
