document.addEventListener('DOMContentLoaded', function () {
    const video = document.getElementById('video');
    const videoName = document.getElementById('videoName');
    const videoDate = document.getElementById('videoDate');
    const videoListContent = document.getElementById('videoListContent');
    const toggleList = document.getElementById('toggleList');
    const videoList = document.getElementById('videoList');
    const prevPage = document.getElementById('prevPage');
    const nextPage = document.getElementById('nextPage');
    const pageInfo = document.getElementById('pageInfo');
    const openListBtn = document.getElementById('openListBtn');
    const videoPlayer = document.getElementById('videoPlayer');
    const selectPrompt = document.getElementById('selectPrompt');
    const noVideosMessage = document.getElementById('noVideosMessage');
    const pagination = document.getElementById('pagination');

    let currentPage = 0;
    const pageSize = 10;
    let totalVideos = 0;

    toggleList.addEventListener('click', () => {
        videoList.classList.toggle('w-80');
        videoList.classList.toggle('w-0');
        videoList.classList.toggle('opacity-0');
        if (videoList.classList.contains('w-0')) {
            openListBtn.classList.remove('hidden');
        } else {
            openListBtn.classList.add('hidden');
        }
    });

    openListBtn.addEventListener('click', () => {
        videoList.classList.add('w-80');
        videoList.classList.remove('w-0');
        videoList.classList.remove('opacity-0');
        openListBtn.classList.add('hidden');
    });

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

    function updateVideoList(videos) {
        videoListContent.innerHTML = '';
        
        if (videos.length === 0) {
            videoListContent.classList.add('hidden');
            noVideosMessage.classList.remove('hidden');
            pagination.classList.add('hidden');
            return;
        }

        videoListContent.classList.remove('hidden');
        noVideosMessage.classList.add('hidden');
        pagination.classList.remove('hidden');

        videos.forEach(video => {
            const videoItem = document.createElement('div');
            videoItem.className = 'p-3 bg-white rounded shadow-sm hover:bg-gray-50 cursor-pointer';
            videoItem.innerHTML = `
                <h3 class="font-medium text-gray-800">${formatVideoName(video.name)}</h3>
                <p class="text-sm text-gray-600">${new Date(video.created).toLocaleDateString()}</p>
            `;
            videoItem.addEventListener('click', () => playVideo(video));
            videoListContent.appendChild(videoItem);
        });
    }

    function playVideo(videoData) {
        const videoUrl = `http://localhost:1221/videos/${videoData.name}`;
        video.src = videoUrl;
        videoName.textContent = formatVideoName(videoData.name);
        videoDate.textContent = `Created on ${new Date(videoData.created).toLocaleDateString()}`;
        video.load();
        videoPlayer.classList.remove('hidden');
        selectPrompt.classList.add('hidden');
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

    video.addEventListener('loadedmetadata', () => {
        console.log('Video metadata loaded');
        console.log('Duration:', video.duration);
        console.log('Video size:', video.videoWidth, 'x', video.videoHeight);
    });

    video.addEventListener('error', (e) => {
        console.error('Video error:', video.error);
    });

    video.addEventListener('progress', () => {
        console.log('Buffered ranges:', video.buffered.length);
        for (let i = 0; i < video.buffered.length; i++) {
            console.log(`Range ${i}:`, video.buffered.start(i), '-', video.buffered.end(i));
        }
    });
});