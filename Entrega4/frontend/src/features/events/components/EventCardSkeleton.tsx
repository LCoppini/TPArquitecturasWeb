import { Skeleton } from '@/components/ui/Skeleton'

/** Misma forma que EventCard mientras llegan los datos. */
export const EventCardSkeleton = () => (
  <div className="flex flex-col overflow-hidden rounded-xl border border-line-subtle bg-surface">
    <Skeleton className="h-47.5 rounded-none" />
    <div className="flex flex-col gap-3 px-5 pt-4.5 pb-5">
      <Skeleton className="h-3 w-20" />
      <Skeleton className="h-7 w-4/5" />
      <Skeleton className="h-4 w-3/5" />
      <Skeleton className="mt-6 h-8 w-2/5" />
    </div>
  </div>
)
