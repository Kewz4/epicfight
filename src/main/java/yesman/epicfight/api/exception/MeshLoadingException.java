package yesman.epicfight.api.exception;

public class MeshLoadingException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public MeshLoadingException(String message) {
		super(message);
	}
	
	public MeshLoadingException(String message, Throwable ex) {
		super(message, ex);
	}
}
