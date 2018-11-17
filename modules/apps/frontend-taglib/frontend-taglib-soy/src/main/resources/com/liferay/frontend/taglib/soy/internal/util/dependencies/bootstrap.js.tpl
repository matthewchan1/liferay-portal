Liferay.Loader.require.apply(
	Liferay.Loader,
	$MODULES.concat(
		[
			function(Component) {
				var context = $CONTEXT;

				var componentConfig = {
					destroyOnNavigate: true,
					portletId: context.portletId
				};

				if ($WRAPPER) {
					Liferay.component(
						'$ID',
						new Component.default(context, '#$ID'),
						componentConfig
					);
				}
				else {
					Liferay.component(
						'$ID',
						new Component.default(context),
						componentConfig
					);
				}
			},
			function(error) {
				console.error('Unable to load ' + $MODULES);

				Liferay.fire(
					'soyComponentLoadingError',
					{
						error: error,
						modules: $MODULES
					}
				);
			}
		]
	)
);