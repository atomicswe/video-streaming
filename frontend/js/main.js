document.addEventListener('DOMContentLoaded', function () {
    const video = document.getElementById('video');
    const videoName = document.getElementById('videoName');
    const videoDate = document.getElementById('videoDate');

    // Get video filename from the source
    const videoSource = video.querySelector('source').src;
    const fileName = videoSource.split('/').pop().split('.')[0];

    // Fetch video metadata
    fetch(`http://localhost:1221/videos/${fileName}/metadata`)
        .then(response => response.json())
        .then(data => {
            // Display video name
            videoName.textContent = data.name || fileName;
            
            // Format and display creation date
            if (data.createdAt) {
                const date = new Date(data.createdAt);
                videoDate.textContent = `Created on ${date.toLocaleDateString()} at ${date.toLocaleTimeString()}`;
            }
        })
        .catch(error => {
            console.error('Error fetching video metadata:', error);
            videoName.textContent = fileName;
            videoDate.textContent = 'Creation date not available';
        });

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