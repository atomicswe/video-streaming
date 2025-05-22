# video-streaming

## Optimize video file with metadata in the beggining:
```bash
ffmpeg -i file-name -movflags faststart -codec copy output-file-name 
```
