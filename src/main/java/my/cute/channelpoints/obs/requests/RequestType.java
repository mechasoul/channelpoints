package my.cute.channelpoints.obs.requests;

public enum RequestType {

	//general
	GetAuthRequired,
	Authenticate,
	OpenProjector,
	
	//media control
	PlayPauseMedia,
	RestartMedia,
	NextMedia,
	SetMediaTime,
	
	//source
	GetMediaSourcesList,
	CreateSource,
	SetVolume,
	GetSourceSettings,
	
	//scene item
	GetSceneItemList,
	SetSceneItemProperties,
	DeleteSceneItem,
	
	//scene
	GetCurrentScene,
	
	//studio mode
	GetStudioModeStatus,
	TransitionToProgram
	
}
