package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Collection
import java.util.logging.Logger

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

/**
 * Clones repository as chained input to specified destination.
 * <p>
 * If destination is not specified, temporary directory is created dynamically and used afterwards. Destination directory has to
 * be empty. In case you specify it and it does not exist, there is an attempt to create it.
 * </p>
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitCloneTool extends Tool<URI, File> {

    private Logger logger = Logger.getLogger(GitCloneTool.class.getName())

    private File destination = null

    private File gitSsh

    @Override
    protected Collection<String> aliases() {
        ["git_clone"]
    }

    /**
     * 
     * @param destination destination where to clone a repository
     * @return
     */
    GitCloneTool destination(String destination) {
        destination(new File(destination))
    }

    /**
     * 
     * @param destination destination where to clone a repository
     * @return
     */
    GitCloneTool destination(File destination) {
        this.destination = destination.getCanonicalFile()
        this
    }

    /**
     * 
     * @param gitSsh file to use as GIT_SSH script, skipped when it does not exist, it is not a file or is a null object
     * @return
     */
    GitCloneTool gitSsh(File gitSsh) {
        if (gitSsh && gitSsh.exists() && gitSsh.isFile() && gitSsh.canExecute()) {
            this.gitSsh = gitSsh
        }
        this
    }
    
    @Override
    protected File process(URI uri) throws Exception {

        if (destination == null) {
            destination = File.createTempFile("spacelift-git-clone-", null)
        }

        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                throw new IllegalStateException(
                String.format("Directory to clone repository (%s) into does not exist and it is unable to create it: %s",
                uri.toString(), destination.getAbsolutePath()))
            }
        }

        if (!destination.isDirectory()) {
            throw new IllegalStateException(
            String.format("Directory you want to clone repository (%s) into is not a directory: %s",
            uri.toString(), destination.getAbsolutePath()))
        }

        if (!destination.canWrite()) {
            throw new IllegalStateException(String.format("Directory (%s) you want to clone repository (%s) into has to be writable.",
            destination, uri.toString()))
        }

        if (destination.list().length != 0) {
            throw new IllegalStateException(
            String.format("Directory you want to clone repository (%s) into is not empty: %s",
            uri.toString(), destination.getAbsolutePath()))
        }

        ProcessResult result = null

        Command command = new CommandBuilder("git").parameters("clone", getAddress(uri), destination.getAbsolutePath()).build()

        logger.info(command.toString())

        try {
            CommandTool clone = Tasks.prepare(CommandTool).command(command)
            
            if (gitSsh) {
                clone.addEnvironment(["GIT_SSH": gitSsh.getAbsolutePath()])
            }
            
            result = clone.execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Unable to clone %s to %s, exit value: %s", uri.toString(), destination.getAbsolutePath(), result.exitValue()),
                ex.getMessage())
            }
        }

        destination
    }

    private def getAddress(URI uri) {
        String address = uri.toString()

        if (address.startsWith("file:/") && !address.startsWith("file:///")) {
            address = address.replace("file:/", "file:///")
        }

        address
    }
}
