(define (problem logistics-4-0)
(:domain logistics)
(:objects
 pos1 pos2 - place
 obj1 obj2 - physobj)

(:init (at obj1 pos1) (at obj2 pos2))

(:goal (and (at obj1 pos2) (at obj2 pos1)))
)