# Video Streaming Platform

## What is this?

This is a very simple video streaming platform that allows users to upload, stream, and manage video content. The platform consists of two main components:

- **Frontend**: A web-based interface built with HTML, JavaScript, and CSS that provides a user-friendly way to interact with the video streaming service.
- **Backend**: A Java-based Spring Boot application that handles video processing, storage, and streaming functionality.

Key features include:
- Video upload and processing
- Adaptive video streaming with range requests support
- Video metadata management
- Paginated video listing
- Background video processing

## How to Use

### Prerequisites

- Java 21 or later
- Python 3.13
- Maven (included via Maven wrapper)
- Make (for using the provided Makefiles)

### Running the Application

1. Clone the repository:
```bash
git clone git@github.com:atomicswe/video-streaming.git
cd video-streaming
```

2. Start the services using the provided Makefile:

To run both frontend and backend:
```bash
make all
```

Or run them separately:
```bash
# Run only the frontend
make frontend

# Run only the backend
make backend
```

The services will be available at:
- Frontend: http://localhost:80
- Backend: http://localhost:1221

### Available Make Commands

#### Root Directory
- `make all` - Start both frontend and backend services
- `make frontend` - Start only the frontend service
- `make backend` - Start only the backend service

#### Backend Directory
- `make build` - Build the backend project
- `make run` - Run the backend application
- `make clean` - Clean the backend project
- `make build-and-run` - Build and run the backend application
- `make help` - Show available backend commands

#### Frontend Directory
- `make run` - Run the frontend service
- `make help` - Show available frontend commands

### API Endpoints

The backend provides the following main endpoints:

- `POST /videos/upload` - Upload a new video
- `GET /videos` - List all videos (supports pagination)
- `GET /videos/{fileName}` - Stream a video
- `GET /videos/{fileName}/metadata` - Get video metadata
- `DELETE /videos/{fileName}` - Delete a video
- `GET /videos/upload/status/{jobId}` - Check video processing status

## How to Contribute

1. Fork the repository
2. Create a new branch for your feature:
```bash
git checkout -b feature/your-feature-name
```

3. Make your changes and commit them:
```bash
git commit -m "Add your feature description"
```

4. Push to your fork:
```bash
git push origin feature/your-feature-name
```

5. Create a Pull Request

### Development Guidelines

- Follow the existing code style and structure
- Write meaningful commit messages
- Update documentation as needed
