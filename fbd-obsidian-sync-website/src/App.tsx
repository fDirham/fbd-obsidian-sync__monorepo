function App() {
  return (
    <main>
      <div className="hero">
        <h1 className="hero__title">FBD Obsidian Sync</h1>
        <p className="hero__blurb">
          An open source cross-platform syncing plugin for{" "}
          <a href="https://obsidian.md/" target="_blank">
            Obsidian.md
          </a>
        </p>
        <div className="hero__actions">
          <a
            href="https://github.com/fDirham/fbd-obsidian-sync__plugin"
            target="_blank"
          >
            Plugin
          </a>
          <a
            href="https://github.com/fDirham/fbd-obsidian-sync__monorepo"
            target="_blank"
          >
            Backend
          </a>
        </div>
      </div>
      <div className="faq">
        <h2>FAQ</h2>
        <h3>How do I use this?</h3>
        <ol>
          <li>Install the plugin (works on mobile too!).</li>
          <li>Open the plugin settings and create an account or log in.</li>
          <li>Create a new "vault" inside the plugin settings.</li>
          <li>Click upload to create a backup.</li>
          <li>Click restore to download a backup.</li>
        </ol>
        <h3>How does syncing work?</h3>
        <p>
          The plugin reads all files in your vault (not including .obsidian),
          zips it, and uploads it to the cloud. When you want to sync, it
          retrieves said files and rebuilds your entire vault.
        </p>
        <p>
          Future versions will be more complex by calculating changesets between
          syncs to allow for auto syncing, but this basic logic is good enough.
        </p>
        <h3>What was this built with?</h3>
        <p>
          <ul>
            <li>The front end is just a pure Obsidian plugin.</li>
            <li>
              The backend is a Java Springboot API hosted in an AWS lambda. Also
              use DynamoDB, S3, Cognito, and API Gateway.
            </li>
          </ul>
        </p>
      </div>
    </main>
  );
}

export default App;
