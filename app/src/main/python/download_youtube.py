from pytubefix import YouTube, Playlist

def download_audio(youtube_link, output_dir):
    try:
        yt = YouTube(youtube_link)
        # Get the highest bitrate audio stream
        audio_stream = yt.streams.filter(only_audio=True).order_by('abr').desc().first()
        sanitized_title = sanitize_filename(yt.title)
        # Determine the file extension (e.g., 'webm', 'm4a')
        file_extension = audio_stream.mime_type.split('/')[1]
        # Download the audio stream in its original format
        audio_file = audio_stream.download(output_path=output_dir, filename=f"{sanitized_title}_audio.{file_extension}")
        print(f"Download successful: {audio_file}")
        return audio_file  # Return the path to the downloaded audio file
    except Exception as e:
        print(f"Attempt failed: {e}")
        return ""  # Return an empty string on failure

def is_youtube_link_valid(url):
    try:
        yt = YouTube(url)
        title = yt.title  # Attempt to access a video attribute
        return "valid"
    except Exception as e:
        if "regex" in str(e):
            return "invalid_regex"
        else:
            return "invalid_failed"

def is_youtube_playlist_link_valid(url):
    try:
        playlist = Playlist(url)
        title = playlist.title  # Attempt to access a playlist attribute
        return "valid"
    except Exception as e:
        if "regex" in str(e):
            return "invalid_regex"
        else:
            return "invalid_failed"

def extract_playlist_links(playlist_url):
    try:
        playlist = Playlist(playlist_url)
        links = [video.watch_url for video in playlist.videos]
        return links
    except Exception as e:
        print(f"Failed to extract playlist links: {e}")
        return []


def sanitize_filename(filename):
    return "".join(c if c.isalnum() or c in (' ', '.', '_') else '_' for c in filename)

def get_available_resolutions(youtube_link):
    try:
        yt = YouTube(youtube_link)
        video_streams = yt.streams.filter(file_extension='mp4', progressive=False).order_by('resolution').desc()
        unique_resolutions = []

        for stream in video_streams:
            if stream.resolution not in unique_resolutions:
                unique_resolutions.append(stream.resolution)

        return unique_resolutions  # Return list of resolution strings

    except Exception as e:
        print(f"Failed to get resolutions: {e}")
        return []  # Return empty list on failure

def get_highest_resolution(youtube_link):
    try:
        yt = YouTube(youtube_link)
        video_streams = yt.streams.filter(file_extension='mp4', progressive=False).order_by('resolution').desc()
        highest_resolution_stream = video_streams.first()
        if highest_resolution_stream:
            resolution = highest_resolution_stream.resolution
            print(f"Highest resolution found: {resolution}")
            return resolution
        else:
            print("No video streams found.")
            return None
    except Exception as e:
        print(f"Failed to get highest resolution: {e}")
        return None

def extract_playlist_links_and_titles(playlist_url):
    try:
        playlist = Playlist(playlist_url)
        video_info_list = []
        for video in playlist.videos:
            video_info = {'link': video.watch_url, 'title': video.title}
            video_info_list.append(video_info)
        return video_info_list
    except Exception as e:
        print(f"Failed to extract playlist links and titles: {e}")
        return []




def download_video_no_audio(youtube_link, resolution, output_dir):
    try:
        yt = YouTube(youtube_link)
        video_stream = yt.streams.filter(file_extension='mp4', progressive=False, res=resolution).first()
        if video_stream is None:
            print(f"No video stream found for resolution {resolution}")
            return ""
        sanitized_title = sanitize_filename(yt.title)
        video_file = video_stream.download(output_path=output_dir, filename=f"{sanitized_title}_video.mp4")
        print(f"Video download successful: {video_file}")
        return video_file
    except Exception as e:
        print(f"Attempt failed: {e}")
        return ""

def download_video_audio(youtube_link, output_dir):
    try:
        yt = YouTube(youtube_link)
        audio_stream = yt.streams.filter(only_audio=True).order_by('abr').desc().first()
        if audio_stream is None:
            print("No audio stream found")
            return ""
        sanitized_title = sanitize_filename(yt.title)
        file_extension = audio_stream.mime_type.split('/')[1]
        audio_file = audio_stream.download(output_path=output_dir, filename=f"{sanitized_title}_audio.{file_extension}")
        print(f"Audio download successful: {audio_file}")
        return audio_file
    except Exception as e:
        print(f"Attempt failed: {e}")
        return ""
