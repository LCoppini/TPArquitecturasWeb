import { Button } from '@/components/ui/Button'
import { ScenePanel } from '@/components/layout/ScenePanel'

/** Portada de la cartelera: título, bajada y acceso directo a los eventos. */
export const CatalogHero = () => (
  <ScenePanel aria-labelledby="hero-title" role="region" className="flex min-h-112 flex-col justify-center gap-6 px-16 py-18" artClassName="top-1/2 -right-10 -mt-65">
    <div className="relative flex max-w-190 flex-col gap-5">
      <span className="text-overline text-brand-text">Cartelera</span>
      <h1 id="hero-title" className="text-display">
        Tu noche, tu entrada
      </h1>
      <p className="max-w-140 text-body-l text-fg-secondary">
        Recitales, teatro y deportes en todo el país. Elegí tu sector, comprá en pocos pasos y recibí tus entradas al instante.
      </p>
      <div>
        <Button asChild size="lg">
          <a href="#cartelera">Ver cartelera</a>
        </Button>
      </div>
    </div>
  </ScenePanel>
)
